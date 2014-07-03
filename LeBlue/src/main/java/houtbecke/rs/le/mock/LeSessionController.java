package houtbecke.rs.le.mock;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import houtbecke.rs.le.LeCharacteristicListener;
import houtbecke.rs.le.LeDeviceListener;
import houtbecke.rs.le.LeFormat;
import houtbecke.rs.le.LeGattCharacteristic;
import houtbecke.rs.le.LeGattStatus;
import houtbecke.rs.le.LeRemoteDeviceListener;
import houtbecke.rs.le.LeUtil;
import houtbecke.rs.le.session.Event;
import houtbecke.rs.le.session.EventSource;
import houtbecke.rs.le.session.EventType;
import houtbecke.rs.le.session.MockedResponse;
import houtbecke.rs.le.session.Mocker;
import houtbecke.rs.le.session.Session;

import static houtbecke.rs.le.session.EventType.*;

public class LeSessionController implements LeMockController {

    // temp workaround for LogCat crashing tests
    boolean shouldLog() { return !"true".equals(System.getProperty("doNotLog"));}

    Handler handler = null;

    final static String TAG = "LeBlueController";
    int counter = 0;
    boolean strict;

    public LeSessionController(Session session) {
        this(session, false);
    }
    public LeSessionController(Session session, boolean strict) {
        this.strict = strict;

        this.session = session;
        try {
            handler = new Handler(Looper.getMainLooper());
        } catch (Exception e) {
            // Failure of the handler to initialize means we're probably running
            // in a JDK environment
            System.out.println("Not using handler for posting events. You're likely running in a JDK environment");
        }
    }

    int source;
    String[] values;
    protected String eventValue() {
        return eventValue(0);
    }
    protected String eventValue(int seq) {
        return values[seq];
    }

    protected int eventIntValue() {
        return Integer.parseInt(values[0]);
    }

    protected boolean eventBooleanValue() {
        return eventBooleanValue(0);
    }
    protected boolean eventBooleanValue(int seq) {
        return Boolean.parseBoolean(eventValue(seq));
    }

    private String point = "";

    public synchronized void pointReached(String point) {
        this.point = point;
        this.notifyAll();
    }

    protected synchronized void waitForPointOrEvent(String point) throws InterruptedException {
        waitingForEvent = true;
        this.notifyAll();
        while (!point.equals(this.point) && sessionIsRunning && !stopSession  && currentEvent != null && currentEvent.type == mockWaitForPoint) {
            this.wait();
        }
        waitingForEvent = false;
        updateCurrentEvent(null);
    }

    protected synchronized void updateCurrentEvent(Event newCurrentEvent) {
        currentEvent = newCurrentEvent;
        this.notifyAll();
    }

    protected void checkPause() {
        long current = System.currentTimeMillis();
        while (current < executeNextEventAfter && !stopSession) {
            if (shouldLog()) Log.i(TAG, "delaying "+currentEvent+" by "+(executeNextEventAfter - current)+ "ms");
            try {
                wait(executeNextEventAfter - current);
            } catch (InterruptedException ignore) {
            }
            current = System.currentTimeMillis();
        }

    }

    public synchronized void waitForPoint(String point) {
        while (!point.equals(this.point) && sessionIsRunning && !stopSession) {
            try {
                this.wait();
            } catch (InterruptedException ignore) {
            }
        }
    }


    Event currentEvent;
    boolean waitingForEvent = false;

    public synchronized boolean waitTillSessionStarted() {
        while (!waitingForEvent && !stopSession) {
            try {
                this.wait(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return waitingForEvent;
    }

    public synchronized void waitForEvent(Event event) throws InterruptedException {
        while (currentEvent != null && sessionIsRunning && !stopSession)
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        updateCurrentEvent(event);
        checkPause();

        while (currentEvent == event && mockedEvents.size() == 0 && sessionIsRunning && !stopSession)
            try {
                waitingForEvent = true;
                this.notifyAll();
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        waitingForEvent = false;
        this.notifyAll();

    }


    String sessionName = null;
    public synchronized void startDefaultSession() {
        startSession(null);
    }

    public synchronized void startSession(String sessionName) {
        stopSession();
        this.waitForFinishedSession();
        stopSession = false;

        this.sessionName = sessionName;
        this.notifyAll();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName("LeSessionController");
                startSessionInThread();

            }
        }).start();

    }

    private class RunnableWrapper implements Runnable {

        final Runnable runnable;
        public RunnableWrapper(Runnable runnable) {
            this.runnable = runnable;
        }
        public boolean started = false;

        @Override
        public void run() {
            synchronized (LeSessionController.this) {
                started = true;
                updateCurrentEvent(null);
                LeSessionController.this.notifyAll();
            }

            runnable.run();
        }
    }

   synchronized void runCurrentEventOnUiThread(final Runnable runnable) throws InterruptedException {

       RunnableWrapper wrapper = null;

       if (handler != null) {

           // robolectric workaround, post to handler from a separate thread
           wrapper = new RunnableWrapper(new Runnable() {
               @Override
               public void run() {
                   handler.post(runnable);
               }
           });
           new Thread(wrapper).start();
       }
       else {

           wrapper = new RunnableWrapper(runnable);
           (new Thread(wrapper)).start();
       }

       synchronized (this) {
           while (!wrapper.started)
               wait();
       }
   }

    Session session;

    volatile List<Event> mockedEvents = new ArrayList<>();
    volatile Event stackedEvent = null;

    boolean sessionIsRunning = false;
    boolean stopSession = false;

    long executeNextEventAfter = 0;

    Map<Integer, byte[]> characteristicsValues = new HashMap<>();

    protected synchronized void startSessionInThread() {
        characteristicsValues.clear();
        sessionIsRunning = true;
        sessionException = null;
        this.notifyAll();

        EventSource source = (sessionName == null) ? session.getDefaultSource() : session.getNamedEventSource(sessionName);
        source.reset();

        Event event = null;

        try {

            while (!stopSession && (source.hasMoreEvent() || mockedEvents.size() > 0 || stackedEvent != null)) {

                if (mockedEvents.size() > 0) {
                    event = mockedEvents.remove(0);
                }
                else if (stackedEvent != null) {
                    event = stackedEvent;
                    stackedEvent = null;
                }
                else {
                    event = source.nextEvent();
                }

                if (stopSession)
                    return;

                workOnEvent(event);

            }
        } catch (Exception e) {
            sessionException = e;
            throw new RuntimeException("error processing session at event "+event, e);
        }
        finally {
            sessionIsRunning = false;
            stopSession = true;
            updateCurrentEvent(null);
        }
    }

    protected void workOnEvent(final Event event) throws InterruptedException {
        if (shouldLog()) Log.i(TAG, "Working on event " + event + " (current event: " + currentEvent + ")");

        switch (event.type) {
            case deviceAddListener:
            case deviceStartScanning:
            case deviceStopScanning:
            case remoteDeviceClose:
            case remoteDeviceDisconnect:
            case serviceEnableCharacteristicNotification:
            case deviceRemoveListener:
            case deviceCheckBleHardwareAvailable:
            case deviceIsBtEnabled:
            case remoteDeviceGetAddress:
            case remoteDeviceGetName:
            case remoteDeviceAddListener:
            case remoteDeviceRemoveListener:
            case remoteDeviceConnect:
            case remoteDeviceStartServiceDiscovery:
            case serviceGetUUID:
            case serviceGetCharacteristic:
            case characteristicGetValue:
            case characteristicGetIntValue:
            case remoteDeviceSetCharacteristicListener:
            case characteristicSetValue:
                waitForEvent(event);
                break;

            default:
                // events that need to update the current event first
                updateCurrentEvent(event);
                checkPause();

                switch (event.type) {

                    case mockRemoteDeviceFound:
                        runCurrentEventOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                for (LeDeviceListener leListener : session.getDeviceMocker(event.source).getDeviceListeners(LeSessionController.this, event.source)) {
                                    leListener.leDeviceFound(
                                            getDevice(event.source),
                                            createOrReturnRemoteDevice(Integer.valueOf(event.values[0]), getDevice(event.source)),
                                            Integer.valueOf(event.values[1]),
                                            LeUtil.stringToBytes(event.values[2]));
                                }
                            }
                        });
                        break;
                    case remoteDeviceFound:
                        runCurrentEventOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                getDeviceListener(event.source).leDeviceFound(
                                        getDevice(event.values[0]),
                                        createOrReturnRemoteDevice(Integer.valueOf(event.values[1]), getDevice(event.source)),
                                        Integer.valueOf(event.values[2]),
                                        LeUtil.stringToBytes(event.values[3]));
                            }
                        });
                        break;

                    case mockRemoteDeviceConnected:
                        for (LeRemoteDeviceListener leRemoteListener : session.getRemoteDeviceMocker(event.source).getRemoteDeviceListeners(this, event.source)) {
                            final LeRemoteDeviceListener listener = leRemoteListener;
                            runCurrentEventOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    listener.leDevicesConnected(getDevice(event.values[0]),
                                            getRemoteDevice(event.source));
                                }
                            });
                        }
                        break;
                    case remoteDeviceConnected:
                        runCurrentEventOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                getRemoteDeviceListener(event.source).leDevicesConnected(
                                        getDevice(event.values[0]),
                                        getRemoteDevice(event.values[1]));
                            }
                        });
                        break;


                    case mockRemoteDeviceServicesDiscovered:
                        for (LeRemoteDeviceListener leRemoteListener : session.getRemoteDeviceMocker(event.source).getRemoteDeviceListeners(this, event.source)) {
                            final LeRemoteDeviceListener listener = leRemoteListener;
                            runCurrentEventOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    LeGattServiceMock[] services = new LeGattServiceMock[event.values.length - 2];
                                    for (int k = 0; k < services.length; k++)
                                        services[k] = createGattService(event.values[k + 2]);

                                    listener.serviceDiscovered(
                                            getDevice(event.values[0]),
                                            getRemoteDevice(event.source),
                                            LeGattStatus.fromString(event.values[1]),
                                            services);
                                }
                            });
                        }
                        break;

                    case remoteDeviceServicesDiscovered:
                        runCurrentEventOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                LeGattServiceMock[] services = new LeGattServiceMock[event.values.length - 3];
                                for (int k = 0; k < services.length; k++)
                                    services[k] = createGattService(event.values[k + 3]);

                                getRemoteDeviceListener(event.source).serviceDiscovered(
                                        getDevice(event.values[0]),
                                        getRemoteDevice(event.values[1]),
                                        LeGattStatus.fromString(event.values[2]),
                                        services);
                            }
                        });
                        break;

                    case mockCharacteristicChangedWithMockedValue:
                        characteristicsValues.put(Integer.valueOf(event.values[0]), LeUtil.stringToBytes(event.values[1]));
                        // fall through
                    case mockCharacteristicChanged:

                        final LeGattCharacteristic characteristic = createOrReturnCharacteristic(event.values[0]);
                        final UUID uuid = UUID.fromString(session.getSourceIdentification(Integer.valueOf(event.values[0])));

                        runCurrentEventOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                for (LeCharacteristicListener leCharacteristicListener : session.getRemoteDeviceMocker(event.source).getCharacteristicListeners(LeSessionController.this, event.source)) {
                                    leCharacteristicListener.leCharacteristicChanged(
                                            uuid,
                                            getRemoteDevice(event.source),
                                            characteristic
                                    );
                                }
                            }
                        });
                        break;

                    case characteristicChanged:
                        runCurrentEventOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                UUID uuid = null;
                                if (event.values[0] != null && !event.values[0].equals("null"))
                                    uuid = UUID.fromString(event.values[0]);
                                getCharacteristicListener(event.source).leCharacteristicChanged(
                                        uuid,
                                        getRemoteDevice(event.values[1]),
                                        getCharacteristic(event.values[2])
                                );
                            }
                        });
                        break;


                    default:
                        // events that also need to unset the current event

                        switch (event.type) {

                            case mockCharacteristicMockedValue:
                                characteristicsValues.put(event.source, LeUtil.stringToBytes(event.values[0]));
                                break;
                            case mockCharacteristicClearMockedValue:
                                characteristicsValues.remove(event.source);
                                break;

                            case mockWaitForPoint:
                                waitForPointOrEvent(event.values[0]);
                                break;

                            case mockPointReached:
                                pointReached(event.values[0]);
                                break;

                            default:
                                throw new RuntimeException("Unimplemented event type: " + event.type);

                        }
                        updateCurrentEvent(null);
                }
        }

        if (shouldLog()) Log.d(TAG, event + " processed");
        executeNextEventAfter = System.currentTimeMillis() + event.delay;
    }

    public synchronized void waitForFinishedSession() {
        while (sessionIsRunning)
            try {
                wait();
            } catch (InterruptedException ignored) {
            }
    }

    public synchronized void stopSession() {
        stopSession = true;
        this.notifyAll();
    }

    Exception sessionException = null;
    public Exception getSessionException() {
        return sessionException;
    }
    
    public boolean checkEvent(EventType event, LeDeviceMock source, String... arguments) {
        return checkEventWithSourceId(event, SourceType.device, getDeviceKey(source), arguments);
    }

    public boolean checkEvent(EventType event, LeRemoteDeviceMock source, String... arguments) {
        return checkEventWithSourceId(event, SourceType.remoteDevice, getRemoteDeviceKey(source), arguments);
    }

    public boolean checkEvent(EventType event, LeGattServiceMock source, String... arguments) {
        return checkEventWithSourceId(event, SourceType.gattService, getGattServiceKey(source), arguments);
    }

    public boolean checkEvent(EventType event, LeGattCharacteristicMock source, String... arguments) {
        return checkEventWithSourceId(event, SourceType.gattCharacteristic, getCharacteristicKey(source), arguments);
    }

    public Session getSession() {
        return session;
    }

    protected static enum SourceType {
        device,
        remoteDevice,
        gattService,
        gattCharacteristic
    }

    public synchronized boolean checkEventWithSourceId(EventType eventType, SourceType sourceType, int source, String... arguments) {

        if (eventType == characteristicGetValue) {
            byte[] value = characteristicsValues.get(source);
            if (value != null) {
                values = new String[]{
                        LeUtil.bytesToString(value)
                };
                return true;
            }
        }

        if (eventType == characteristicGetIntValue) {
            byte[] value = characteristicsValues.get(source);
            if (value != null) {
                values = new String[]{
                        value[0]+""
                };
                return true;
            }
        }


        Mocker mocker;
        switch (sourceType) {
            case device:
                mocker = session.getDeviceMocker(source);
                break;
            case remoteDevice:
                mocker = session.getRemoteDeviceMocker(source);
                break;
            case gattService:
                mocker = session.getGattServiceMocker(source);
                break;
            case gattCharacteristic:
                mocker = session.getGattCharacteristicMocker(source);
                break;
            default:
                mocker = null;
                break;
        }

        if (mocker != null) {
            MockedResponse mockedResponse = mocker.mock(this, eventType, source, arguments);

            // if we can mock the event, for the event loop nothing happens (the current event stays the same)
            // however, if we have created a mocked event, we will wake up the event loop to process it. It will stack the current event and restore
            // it after the mocked event has been dealt with.
            //
            // to ensure smooth execution the method will only return after the mocked event has actually been processed.

            if (mockedResponse != null) {

                mockedEvents.addAll(Arrays.asList(mockedResponse.getNextMockedEvents()));
                values = mockedResponse.getMockedResultValues();
                if (mockedEvents.size() > 0) {
                    while (sessionIsRunning && currentEvent != null && stackedEvent != null)
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    if (currentEvent != null)
                        stackedEvent = currentEvent;

                    updateCurrentEvent(null);

                    while (sessionIsRunning && (currentEvent == null || mockedEvents.size() > 0))
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                }
                return true;

            }
        }

        while (!waitingForEvent && sessionIsRunning)
            try {
                this.wait();
            } catch (InterruptedException ignored) {
            }

        if (currentEvent != null && eventType == currentEvent.type) {
            this.source = currentEvent.source;
            this.values = currentEvent.values;
            updateCurrentEvent(null);
            while (currentEvent == null && sessionIsRunning)
                try {
                    wait();
                } catch (InterruptedException ignored) {
                }

            if (this.source == source) {
                if (shouldLog()) Log.i(TAG, eventType + "("+source+") is happening " + Arrays.toString(this.values));

                // strict checking disabled for arguments for now. Right now there is one source, and arguments.
                // For this to work that should be refactored to a path of sources and arguments.

//                if (strict && !Arrays.deepEquals(values, arguments) && arguments.length != 0) {
//                    String message = "actual values "+Arrays.toString(values)+" not equal to expected arguments "+Arrays.toString(arguments);
//                    if (shouldLog()) Log.i(TAG, message);
//                    throw new RuntimeException(message);
//                }
                return true;
            }
            else {
                String message = "Mismatch source: For event "+ eventType +" source not correct: "+source+" expected "+this.source;
                if (strict)
                    throw new RuntimeException(message);
                if (shouldLog()) Log.w(TAG, message);
                return false;
            }
        }
        String message = "Mismatch, expected "+ (currentEvent != null ? currentEvent.type : "nothing") + " got :"+ eventType + "(" + source + ") is happening (session running? :" + sessionIsRunning + ") with values" + Arrays.toString(this.values) + " full event: " + currentEvent;
        if (strict)
            throw new RuntimeException(message);
        if (shouldLog()) Log.w(TAG, message);
        return false;
    }

    @Override
    public synchronized void deviceStartScanning(LeDeviceMock leDeviceMock) {
        checkEvent(deviceStartScanning, leDeviceMock);
    }

    @Override
    public synchronized void deviceStartScanning(LeDeviceMock leDeviceMock, UUID[] uuids) {
        checkEvent(deviceStartScanning, leDeviceMock);
    }

    @Override
    public synchronized void deviceStopScanning(LeDeviceMock leDeviceMock) {
        checkEvent(deviceStopScanning, leDeviceMock);
    }

    @Override
    public synchronized void remoteDeviceConnect(LeRemoteDeviceMock leRemoteDeviceMock) {
        checkEvent(remoteDeviceConnect, leRemoteDeviceMock);
    }

    @Override
    public synchronized void remoteDeviceStartServiceDiscovery(LeRemoteDeviceMock leRemoteDeviceMock) {
        checkEvent(remoteDeviceStartServiceDiscovery, leRemoteDeviceMock);
    }

    @Override
    public synchronized void remoteDeviceClose(LeRemoteDeviceMock leRemoteDeviceMock) {
        checkEvent(remoteDeviceClose, leRemoteDeviceMock);

    }

    @Override
    public synchronized void remoteDeviceDisconnect(LeRemoteDeviceMock leRemoteDeviceMock) {
        checkEvent(remoteDeviceDisconnect, leRemoteDeviceMock);
    }

    Map<Integer, LeCharacteristicListener> characteristicListeners = new HashMap<>();
    protected LeCharacteristicListener getCharacteristicListener(String key) {
        return getCharacteristicListener(Integer.valueOf(key));
    }
    @Override
    public LeCharacteristicListener getCharacteristicListener(int key) {
        return characteristicListeners.get(key);
    }

    @Override
    public void remoteDeviceSetCharacteristicListener(LeRemoteDeviceMock leRemoteDeviceMock, LeCharacteristicListener listener, UUID[] uuids) {
        checkEvent(remoteDeviceSetCharacteristicListener, leRemoteDeviceMock, Arrays.toString(uuids));
        characteristicListeners.put(eventIntValue(), listener);
    }

    @Override
    public synchronized boolean serviceEnableCharacteristicNotification(LeGattServiceMock leGattServiceMock, UUID characteristic) {
        if (checkEvent(serviceEnableCharacteristicNotification, leGattServiceMock, characteristic.toString()))
            return eventBooleanValue(1);
        else
            return true;
    }

    Map<Integer, LeDeviceMock> devices = new HashMap<>();
    Map<LeDeviceMock, Integer> deviceKeys = new HashMap<>();
    protected LeDeviceMock getDevice(String key) {
        return getDevice(Integer.parseInt(key));
    }
    protected LeDeviceMock getDevice(int key) {
        return devices.get(key);
    }
    protected int getDeviceKey(LeDeviceMock device) {
        return deviceKeys.get(device);
    }

    @Override
    public void addDevice(int key, LeDeviceMock mock) {
        devices.put(key, mock);
        deviceKeys.put(mock, key);
    }

    Map<Integer, LeRemoteDeviceMock> remoteDevices = new HashMap<>();
    Map<LeRemoteDeviceMock, Integer> remoteDeviceKeys = Collections.synchronizedMap(new HashMap<LeRemoteDeviceMock, Integer>());
    protected LeRemoteDeviceMock createRemoteDevice(int key, LeDeviceMock deviceMock) {
        remoteDevices.put(key, new LeRemoteDeviceMock(key, this, deviceMock));
        remoteDeviceKeys.put(remoteDevices.get(key), key);
        return remoteDevices.get(key);
    }
    protected LeRemoteDeviceMock createOrReturnRemoteDevice(int key, LeDeviceMock deviceMock) {
        LeRemoteDeviceMock mock = remoteDevices.get(key);
        return mock != null ? mock : createRemoteDevice(key, deviceMock);
    }

    protected int getRemoteDeviceKey(LeRemoteDeviceMock leRemoteDeviceMock) {
        return remoteDeviceKeys.get(leRemoteDeviceMock);
    }
    protected LeRemoteDeviceMock getRemoteDevice(String key) {
        return getRemoteDevice(Integer.parseInt(key));
    }
    protected LeRemoteDeviceMock getRemoteDevice(int key) {
        return remoteDevices.get(key);
    }


    Map<Integer, LeGattServiceMock> gattServices = new HashMap<>();
    Map<LeGattServiceMock, Integer> gattServicesKeys = new HashMap<>();
    protected LeGattServiceMock createGattService(String key) {
        return createGattService(Integer.valueOf(key));
    }
    protected LeGattServiceMock createGattService(int key) {
        gattServices.put(key, new LeGattServiceMock(key, this));
        gattServicesKeys.put(gattServices.get(key), key);
        return gattServices.get(key);
    }
    protected int getGattServiceKey(LeGattServiceMock LeGattServiceMock) {
        return gattServicesKeys.get(LeGattServiceMock);
    }

    Map<Integer, LeDeviceListener> deviceListeners = new HashMap<>();
    Map<LeDeviceListener, Integer> deviceListenerKeys = new HashMap<>();
    protected LeDeviceListener getDeviceListener(String key) {
        return getDeviceListener(Integer.valueOf(key));
    }
    protected void addDeviceListener(int key, LeDeviceListener listener) {
        deviceListeners.put(key, listener);
        deviceListenerKeys.put(listener, key);
    }
    @Override
    public LeDeviceListener getDeviceListener(int key) {
        return deviceListeners.get(key);
    }
    protected int getDeviceListenerKey(LeDeviceListener deviceListener) {
        return deviceListenerKeys.get(deviceListener);
    }



    Map<Integer, LeGattCharacteristicMock> characteristics = new HashMap<>();
    Map<LeGattCharacteristicMock, Integer> characteristicsKeys = new HashMap<>();
    protected LeGattCharacteristicMock createOrReturnCharacteristic(String key) {
        return createOrReturnCharacteristic(Integer.valueOf(key));
    }
    protected LeGattCharacteristicMock createOrReturnCharacteristic(int key) {
        LeGattCharacteristicMock mock = characteristics.get(key);
        if (mock != null)
            return mock;
        characteristics.put(key, new LeGattCharacteristicMock(this));
        characteristicsKeys.put(characteristics.get(key), key);
        return characteristics.get(key);
    }
    protected int getCharacteristicKey(LeGattCharacteristicMock characteristic) {
        return characteristicsKeys.get(characteristic);
    }
    protected LeGattCharacteristicMock getCharacteristic(String key) {
        return getCharacteristic(Integer.valueOf(key));
    }
    protected LeGattCharacteristicMock getCharacteristic(int key) {
        return characteristics.get(key);
    }

    @Override
    public LeGattCharacteristic serviceGetCharacteristic(LeGattServiceMock leGattServiceMock, UUID uuid) {
        if (checkEvent(serviceGetCharacteristic, leGattServiceMock, uuid.toString()))
            return createOrReturnCharacteristic(eventIntValue());
        else
            return null;
    }

    @Override
    public synchronized void deviceAddListener(LeDeviceMock leDeviceMock, LeDeviceListener listener) {
        if (checkEventWithSourceId(deviceAddListener, SourceType.device, getDeviceKey(leDeviceMock))) {
            addDeviceListener(eventIntValue(), listener);
        }
    }

    @Override
    public synchronized void deviceRemoveListener(LeDeviceMock leDeviceMock, LeDeviceListener listener) {
        checkEvent(deviceRemoveListener, leDeviceMock);
    }

    Map<Integer, LeRemoteDeviceListener> remoteDeviceListeners = new HashMap<>();
    protected LeRemoteDeviceListener getRemoteDeviceListener(String key) {
        return remoteDeviceListeners.get(Integer.parseInt(key));
    }
    @Override
    public LeRemoteDeviceListener getRemoteDeviceListener(int key) {
        return remoteDeviceListeners.get(key);
    }

    @Override
    public void remoteDeviceAddListener(LeRemoteDeviceMock leRemoteDeviceMock, LeRemoteDeviceListener listener) {
        if (checkEvent(remoteDeviceAddListener, leRemoteDeviceMock))
            remoteDeviceListeners.put(eventIntValue(), listener);
    }

    @Override
    public void remoteDeviceRemoveListener(LeRemoteDeviceMock leRemoteDeviceMock, LeRemoteDeviceListener listener) {
        checkEvent(remoteDeviceRemoveListener, leRemoteDeviceMock);
        remoteDeviceListeners.remove(eventIntValue());
    }

    @Override
    public synchronized boolean deviceCheckBleHardwareAvailable(LeDeviceMock leDeviceMock) {
        checkEvent(deviceCheckBleHardwareAvailable, leDeviceMock);
        return eventBooleanValue();
    }

    @Override
    public synchronized boolean deviceIsBtEnabled(LeDeviceMock leDeviceMock) {
        checkEvent(deviceIsBtEnabled, leDeviceMock);
        return eventBooleanValue();
    }

    @Override
    public String remoteDeviceGetAddress(LeRemoteDeviceMock leRemoteDeviceMock) {
        if (checkEvent(remoteDeviceGetAddress, leRemoteDeviceMock))
            return eventValue();
        else
            return "0:0:0:0";
    }

    @Override
    public String remoteDeviceGetName(LeRemoteDeviceMock leRemoteDeviceMock) {
        checkEvent(remoteDeviceGetName, leRemoteDeviceMock);
        return eventValue();
    }

    @Override
    public UUID serviceGetUuid(LeGattServiceMock leGattServiceMock) {
        return checkEvent(serviceGetUUID, leGattServiceMock) ? UUID.fromString(eventValue()) : UUID.randomUUID();
    }

    @Override
    public byte[] characteristicGetValue(LeGattCharacteristicMock leGattCharacteristicMock) {
        checkEvent(characteristicGetValue, leGattCharacteristicMock);
        return LeUtil.stringToBytes(eventValue());
    }

    @Override
    public int characteristicGetIntValue(LeGattCharacteristicMock leGattCharacteristicMock, LeFormat format, int index) {
        checkEvent(characteristicGetIntValue, leGattCharacteristicMock, format.toString(), index+"");
        return eventIntValue();
    }

    @Override
    public void characteristicSetValue(LeGattCharacteristicMock leGattCharacteristicMock, byte[] value) {
        checkEvent(characteristicSetValue, leGattCharacteristicMock, LeUtil.bytesToString(value));
    }

}