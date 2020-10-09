LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := liblept

LOCAL_SRC_FILES += \
  box.cpp \
  boxa.cpp \
  pix.cpp \
  pixa.cpp \
  utilities.cpp \
  readfile.cpp \
  writefile.cpp \
  jni.cpp

LOCAL_EXPORT_CFLAGS := \
  -DHAVE_CONFIG_H

LOCAL_EXPORT_C_INCLUDES := \
  $(LOCAL_PATH) \
  $(LEPTONICA_PATH)/src

LOCAL_C_INCLUDES := \
  $(LOCAL_EXPORT_C_INCLUDES) \
  $(LIBJPEG_PATH) \
  $(LIBPNG_PATH)

LOCAL_LDLIBS += \
  -ljnigraphics \
  -llog

LOCAL_STATIC_LIBRARIES:= liblept_static

include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := liblept_static
LOCAL_THIN_ARCHIVE := true

LOCAL_EXPORT_CFLAGS := \
  -DHAVE_CONFIG_H

LOCAL_CFLAGS := \
  $(LOCAL_EXPORT_CFLAGS) \
  -include $(LOCAL_PATH)/common.h

LOCAL_EXPORT_C_INCLUDES := \
  $(LOCAL_PATH) \
  $(LEPTONICA_PATH)/src

LOCAL_PATH := $(LEPTONICA_PATH)/src

get-src-file-target-cflags += $(if $(filter bmpio.c,$1),-Wno-address-of-packed-member,)

# leptonica (minus freetype)

BLACKLIST_SRC_FILES := \
  %endiantest.c \
  %freetype.c \
  %xtractprotos.c

LEPTONICA_SRC_FILES := \
  $(subst $(LOCAL_PATH)/,,$(wildcard $(LEPTONICA_PATH)/src/*.c))

LOCAL_SRC_FILES := \
  $(filter-out $(BLACKLIST_SRC_FILES),$(LEPTONICA_SRC_FILES))

LOCAL_C_INCLUDES += \
  $(LOCAL_EXPORT_C_INCLUDES) \
  $(LIBPNG_PATH)

LOCAL_SHARED_LIBRARIES:= libpngo
include $(BUILD_STATIC_LIBRARY)