package tests

import houtbecke.rs.le.*
import houtbecke.rs.le.mock.LeDeviceMock
import houtbecke.rs.le.mock.LeSessionController
import houtbecke.rs.le.session.Event
import houtbecke.rs.le.session.EventSinkFiller
import houtbecke.rs.le.session.ListEventSinkSource
import houtbecke.rs.le.session.SessionObject
import org.junit.Before
import org.junit.Test

import static houtbecke.rs.le.session.EventType.*

class MockBluetoothTest  {

    LeSessionController sessionController;
    LeDevice device

    final int LE_DEVICE = 1
    final int LE_REMOTE_DEVICE = 3
    final int LE_DEVICE_LISTENER = 2
    final int LE_REMOTE_DEVICE_LISTENER = 4
    final int LE_SERVICE_1 = 5;
    final int LE_CHARACTERISTIC_1_1 = 6;
    final int LE_CHARACTERISTIC_1_2 = 7;
    final int LE_CHARACTERISTIC_LISTENER = 8;

    @Before
    void setUp() throws Exception {
        System.setProperty "doNotLog", "true"
    }

    def createSource() {
        def source = new ListEventSinkSource();
        def filler = new EventSinkFiller(source)
        filler.addEvent(
            deviceAddListener,    // event
            LE_DEVICE,                              // source
            LE_DEVICE_LISTENER)             // params

        filler.addEvent(deviceStartScanning,
                LE_DEVICE)

        filler.addEvent(remoteDeviceFound,
                LE_DEVICE_LISTENER,
                    LE_DEVICE,
                    LE_REMOTE_DEVICE,
                    "123",
                    "0,1,2")

        filler.addEvent(remoteDeviceGetAddress,
                LE_REMOTE_DEVICE,
                    "0001:0002:0003:0004")

        filler.addEvent(remoteDeviceGetName,
                LE_REMOTE_DEVICE,
                    "test device")

        filler.addEvent(remoteDeviceAddListener,
                LE_REMOTE_DEVICE,
                    LE_REMOTE_DEVICE_LISTENER )

        filler.addEvent(remoteDeviceConnect,
                LE_REMOTE_DEVICE)

        filler.addEvent(remoteDeviceConnected,
                LE_REMOTE_DEVICE_LISTENER,
                    LE_DEVICE,
                    LE_REMOTE_DEVICE)

        filler.addEvent(remoteDeviceStartServiceDiscovery,
                LE_REMOTE_DEVICE)

        filler.addEvent(remoteDeviceServicesDiscovered,
                LE_REMOTE_DEVICE_LISTENER,
                    LE_DEVICE,
                    LE_REMOTE_DEVICE,
                    LeGattStatus.SUCCESS.toString(),
                    "$LE_SERVICE_1")

        filler.addEvent(serviceGetUUID,
                LE_SERVICE_1,
                    UUID.fromString("12345678-1234-1234-1234-123456789aaaa").toString())

        filler.addEvent(serviceGetCharacteristic,
                LE_SERVICE_1,
                    "$LE_CHARACTERISTIC_1_1",
                    UUID.fromString("12345678-1234-1234-1234-123456789bbbb").toString())

        filler.addEvent(serviceGetCharacteristic,
                LE_SERVICE_1,
                "$LE_CHARACTERISTIC_1_2",
                UUID.fromString("12345678-1234-1234-1234-123456789eeee").toString())


        filler.addEvent(characteristicGetValue,
                LE_CHARACTERISTIC_1_1,
                    "0,1,2")

        filler.addEvent(remoteDeviceSetCharacteristicListener,
                LE_REMOTE_DEVICE,
                    "$LE_CHARACTERISTIC_LISTENER",
                    UUID.fromString("12345678-1234-1234-1234-123456789cccc").toString())

        filler.addEvent(serviceEnableCharacteristicNotification,
                LE_SERVICE_1,
                UUID.fromString("12345678-1234-1234-1234-123456789cccc").toString(),
                "true")

        filler.addEvent(characteristicChanged,
                LE_CHARACTERISTIC_LISTENER,
                    UUID.fromString("12345678-1234-1234-1234-123456789cccc").toString(),
                    "$LE_REMOTE_DEVICE",
                    "$LE_CHARACTERISTIC_1_2")

        filler.addEvent(characteristicSetValue,
            LE_CHARACTERISTIC_1_2,
                "10,9,8")

        return source
    }

    LeRemoteDevice remoteDevice

    @Test
    void testController() {
        def events = createSource();
        sessionController = new LeSessionController(SessionObject.newSession().withDefaultSessionSource(events), true)
        device = new LeDeviceMock(sessionController)
        sessionController.startSessionThread()
        assert sessionController.waitForSessionToWait();

        def foundRemoteDevice = false

        device.addListener(new LeDeviceListener() {
            @Override
            void leDeviceFound(LeDevice leDeviceFound, LeRemoteDevice leFoundRemoteDevice, int rssi, byte[] scanRecord) {
                assert device == leDeviceFound
                assert leFoundRemoteDevice != null
                assert rssi == 123
                assert scanRecord == [0, 1, 2]

                remoteDevice = leFoundRemoteDevice

                foundRemoteDevice = true
            }
        })
        device.startScanning()
        assert foundRemoteDevice

        assert remoteDevice.getAddress() == "0001:0002:0003:0004"
        assert remoteDevice.getName() == "test device"

        def connected = false, disconnected = false, closed = false, discovered = false;

        LeGattService service;
        remoteDevice.addListener(new LeRemoteDeviceListener() {

            @Override
            void leDevicesConnected(LeDevice leDeviceFoundOn, LeRemoteDevice leRemoteDevice) {
                assert device == leDeviceFoundOn
                assert remoteDevice == leRemoteDevice
                connected = true;
            }

            @Override
            void leDevicesDisconnected(LeDevice leDevice, LeRemoteDevice leRemoteDevice) {

            }

            @Override
            void leDevicesClosed(LeDevice leDevice, LeRemoteDevice leRemoteDevice) {

            }

            @Override
            void serviceDiscovered(LeDevice leDevice, LeRemoteDevice leRemoteDevice, LeGattStatus status, LeGattService[] gatts) {
                discovered = true;
                assert device == leDevice;
                assert leRemoteDevice == remoteDevice
                assert LeGattStatus.SUCCESS == status;
                assert gatts.length == 1
                service = gatts[0]
            }
        })

        remoteDevice.connect();

        assert connected

        remoteDevice.startServicesDiscovery()

        assert discovered

        service.getUuid() == UUID.fromString("12345678-1234-1234-1234-123456789aaaa")

        def characteristic = service.getCharacteristic(UUID.fromString("12345678-1234-1234-1234-123456789bbbb"))
        assert characteristic != null

        def characteristic2 = service.getCharacteristic(UUID.fromString("12345678-1234-1234-1234-123456789eeee"))
        assert characteristic2 != null

        assert characteristic.getValue() == [0, 1, 2]

        def changed = false

        remoteDevice.setCharacteristicListener(new LeCharacteristicListener() {
            @Override
            void leCharacteristicChanged(UUID uuid, LeRemoteDevice leRemoteDevice, LeGattCharacteristic leCharacteristic) {
                assert uuid == UUID.fromString("12345678-1234-1234-1234-123456789cccc")
                assert remoteDevice == leRemoteDevice
                assert leCharacteristic != characteristic, "make sure this is a different characteristic"
                changed = true;
            }
        }, UUID.fromString("12345678-1234-1234-1234-123456789cccc"))

        service.enableCharacteristicNotification(UUID.fromString("12345678-1234-1234-1234-123456789cccc"))
        assert changed;

        characteristic2.setValue([3, 4, 5] as byte[]);

        assert !events.hasMoreEvent()

        assert sessionController.getSessionException() == null
    }
}