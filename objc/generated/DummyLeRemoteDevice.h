
#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_DummyLeRemoteDevice")
#ifdef RESTRICT_DummyLeRemoteDevice
#define INCLUDE_ALL_DummyLeRemoteDevice 0
#else
#define INCLUDE_ALL_DummyLeRemoteDevice 1
#endif
#undef RESTRICT_DummyLeRemoteDevice

#if !defined (HoutbeckeRsLeDummyDummyLeRemoteDevice_) && (INCLUDE_ALL_DummyLeRemoteDevice || defined(INCLUDE_HoutbeckeRsLeDummyDummyLeRemoteDevice))
#define HoutbeckeRsLeDummyDummyLeRemoteDevice_

#define RESTRICT_LeRemoteDevice 1
#define INCLUDE_LeRemoteDevice 1
#include "LeRemoteDevice.h"

@class IOSObjectArray;
@protocol JavaUtilMap;
@protocol LeCharacteristicListener;
@protocol LeCharacteristicWriteListener;
@protocol LeRemoteDeviceListener;

@interface HoutbeckeRsLeDummyDummyLeRemoteDevice : NSObject < LeRemoteDevice >

#pragma mark Public

- (instancetype)init;

- (void)addListenerWithLeRemoteDeviceListener:(id<LeRemoteDeviceListener>)listener;

- (void)close;

- (void)connect;

- (void)disconnect;

- (NSString *)getAddress;

- (NSString *)getName;

- (void)readRssi;

- (void)refreshDeviceCache;

- (void)removeListenerWithLeRemoteDeviceListener:(id<LeRemoteDeviceListener>)listener;

- (void)setCharacteristicListenerWithLeCharacteristicListener:(id<LeCharacteristicListener>)listener
                                        withJavaUtilUUIDArray:(IOSObjectArray *)uuids;

- (void)setCharacteristicWriteListenerWithLeCharacteristicWriteListener:(id<LeCharacteristicWriteListener>)listener
                                                  withJavaUtilUUIDArray:(IOSObjectArray *)uuids;

- (void)startServicesDiscovery;

- (void)startServicesDiscoveryWithJavaUtilMap:(id<JavaUtilMap>)services;

@end

J2OBJC_EMPTY_STATIC_INIT(HoutbeckeRsLeDummyDummyLeRemoteDevice)

FOUNDATION_EXPORT void HoutbeckeRsLeDummyDummyLeRemoteDevice_init(HoutbeckeRsLeDummyDummyLeRemoteDevice *self);

FOUNDATION_EXPORT HoutbeckeRsLeDummyDummyLeRemoteDevice *new_HoutbeckeRsLeDummyDummyLeRemoteDevice_init() NS_RETURNS_RETAINED;

FOUNDATION_EXPORT HoutbeckeRsLeDummyDummyLeRemoteDevice *create_HoutbeckeRsLeDummyDummyLeRemoteDevice_init();

J2OBJC_TYPE_LITERAL_HEADER(HoutbeckeRsLeDummyDummyLeRemoteDevice)

#endif

#pragma pop_macro("INCLUDE_ALL_DummyLeRemoteDevice")
