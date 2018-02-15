LOCAL_PATH := $(call my-dir)

LEPTONICA_PATH := $(LOCAL_PATH)/libs/leptonica/leptonica
TESSERACT_PATH := $(LOCAL_PATH)/libs/tesseract/tesseract
LIBPNG_PATH := $(LOCAL_PATH)/libs/libpng-android/jni


include $(LIBPNG_PATH)/Android.mk
include $(LEPTONICA_PATH)/../Android.mk
include $(TESSERACT_PATH)/../Android.mk
