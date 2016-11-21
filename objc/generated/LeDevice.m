
#include "J2ObjC_source.h"
#include "LeDevice.h"

@interface LeDevice : NSObject

@end

@implementation LeDevice

+ (const J2ObjcClassInfo *)__metadata {
  static const J2ObjcMethodInfo methods[] = {
    { "addListenerWithLeDeviceListener:", "addListener", "V", 0x401, NULL, NULL },
    { "removeListenerWithLeDeviceListener:", "removeListener", "V", 0x401, NULL, NULL },
    { "checkBleHardwareAvailable", NULL, "Z", 0x401, NULL, NULL },
    { "isBtEnabled", NULL, "Z", 0x401, NULL, NULL },
    { "startScanning", NULL, "V", 0x401, NULL, NULL },
    { "startScanningWithJavaUtilUUIDArray:", "startScanning", "V", 0x481, NULL, NULL },
    { "stopScanning", NULL, "V", 0x401, NULL, NULL },
  };
  static const J2ObjcClassInfo _LeDevice = { 2, "LeDevice", "houtbecke.rs.le", NULL, 0x609, 7, methods, 0, NULL, 0, NULL, 0, NULL, NULL, NULL };
  return &_LeDevice;
}

@end

J2OBJC_INTERFACE_TYPE_LITERAL_SOURCE(LeDevice)