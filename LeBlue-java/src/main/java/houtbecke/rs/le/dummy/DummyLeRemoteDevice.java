package houtbecke.rs.le.dummy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import houtbecke.rs.le.LeCharacteristicListener;
import houtbecke.rs.le.LeCharacteristicWriteListener;
import houtbecke.rs.le.LeRemoteDevice;
import houtbecke.rs.le.LeRemoteDeviceListener;

public class DummyLeRemoteDevice implements LeRemoteDevice {
    @Override
    public void addListener(LeRemoteDeviceListener listener) {

    }

    @Override
    public void removeListener(LeRemoteDeviceListener listener) {

    }

    @Override
    public String getAddress() {
        return null;
    }

    @Override
    public void connect() {

    }

    @Override
    public void disconnect() {

    }

    @Override
    public void close() {

    }

    @Override
    public void startServicesDiscovery() {

    }

    @Override
    public void startServicesDiscovery(Map<UUID, List> services) {
        List uuids = new ArrayList<>(services.keySet());
    }


    @Override
    public void setCharacteristicWriteListener(LeCharacteristicWriteListener listener, UUID... uuids) {

    }

    @Override
    public void setCharacteristicListener(LeCharacteristicListener listener, UUID... uuids) {

    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void readRssi() {

    }

    @Override
    public void refreshDeviceCache() {

    }
}
