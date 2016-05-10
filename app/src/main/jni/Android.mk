
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

HOCR2PDF_PATH := $(LOCAL_PATH)/../../../../hocr2pdf/src
HOCR2PDF_JNI_PATH := $(LOCAL_PATH)/hocr2pdf

IMAGE_PROCESSING_PATH := $(LOCAL_PATH)/../../../../image-processing/src
#IMAGE_PROCESSING_PATH := $(LOCAL_PATH)/../../../../image-processing-private/src

IMAGE_PROCESSING_JNI_PATH := $(LOCAL_PATH)/image_processing

TESS_TWO_PATH := $(LOCAL_PATH)/../../../../tess-two/tess-two
LEPTONICA_SRC_PATH := $(TESS_TWO_PATH)/jni/com_googlecode_leptonica_android/src

LIBJPEG_PATH := $(LOCAL_PATH)/../../../../libjpeg
LIBPNG_PATH := $(LOCAL_PATH)/../../../../libpng-android/jni


include $(LIBJPEG_PATH)/Android.mk
#include $(LIBPNG_PATH)/Android.mk
include $(HOCR2PDF_PATH)/Android.mk
include $(HOCR2PDF_JNI_PATH)/Android.mk
include $(IMAGE_PROCESSING_JNI_PATH)/Android.mk
