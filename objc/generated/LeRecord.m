
#include "J2ObjC_source.h"
#include "LeRecord.h"

@interface LeRecord : NSObject

@end

@implementation LeRecord

+ (const J2ObjcClassInfo *)__metadata {
  static const J2ObjcMethodInfo methods[] = {
    { "getType", NULL, "I", 0x401, NULL, NULL },
    { "getRecordContent", NULL, "[B", 0x401, NULL, NULL },
  };
  static const J2ObjcClassInfo _LeRecord = { 2, "LeRecord", "houtbecke.rs.le", NULL, 0x609, 2, methods, 0, NULL, 0, NULL, 0, NULL, NULL, NULL };
  return &_LeRecord;
}

@end

J2OBJC_INTERFACE_TYPE_LITERAL_SOURCE(LeRecord)