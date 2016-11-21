
#include "J2ObjC_source.h"
#include "LeDeviceListener.h"

@interface LeDeviceListener : NSObject

@end

@implementation LeDeviceListener

+ (const J2ObjcClassInfo *)__metadata {
  static const J2ObjcMethodInfo methods[] = {
    { "leDeviceFoundWithLeDevice:withLeRemoteDevice:withInt:withLeScanRecord:", "leDeviceFound", "V", 0x401, NULL, NULL },
    { "leDeviceStateWithLeDevice:withLeDeviceState:", "leDeviceState", "V", 0x401, NULL, NULL },
  };
  static const J2ObjcClassInfo _LeDeviceListener = { 2, "LeDeviceListener", "houtbecke.rs.le", NULL, 0x609, 2, methods, 0, NULL, 0, NULL, 0, NULL, NULL, NULL };
  return &_LeDeviceListener;
}

@end

J2OBJC_INTERFACE_TYPE_LITERAL_SOURCE(LeDeviceListener)