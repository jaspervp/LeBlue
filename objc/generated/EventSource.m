
#include "EventSource.h"
#include "J2ObjC_source.h"

@interface EventSource : NSObject

@end

@implementation EventSource

+ (const J2ObjcClassInfo *)__metadata {
  static const J2ObjcMethodInfo methods[] = {
    { "nextEvent", NULL, "Lhoutbecke.rs.le.session.Event;", 0x401, NULL, NULL },
    { "hasMoreEvent", NULL, "Z", 0x401, NULL, NULL },
    { "reset", NULL, "V", 0x401, NULL, NULL },
  };
  static const J2ObjcClassInfo _EventSource = { 2, "EventSource", "houtbecke.rs.le.session", NULL, 0x609, 3, methods, 0, NULL, 0, NULL, 0, NULL, NULL, NULL };
  return &_EventSource;
}

@end

J2OBJC_INTERFACE_TYPE_LITERAL_SOURCE(EventSource)