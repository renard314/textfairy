LOCAL_PATH := $(call my-dir)

LEPTONICA_PATH := $(LOCAL_PATH)/libs/leptonica/leptonica
TESSERACT_PATH := $(LOCAL_PATH)/libs/tesseract/tesseract
HOCR2PDF_PATH := $(LOCAL_PATH)/libs/hocr2pdf/hocr2pdf
LIBPNG_PATH := $(LOCAL_PATH)/libs/libpng-android/jni
LIBJPEG_PATH := $(LOCAL_PATH)/libs/libjpeg
IMAGE_PROCESSING_PATH := $(LOCAL_PATH)/libs/image_processing/image_processing
ADAPTIVE_BINARIZER_PATH := $(LOCAL_PATH)/libs/image_processing/image-processing-private


include $(LIBPNG_PATH)/Android.mk
include $(LIBJPEG_PATH)/Android.mk
include $(HOCR2PDF_PATH)/../Android.mk
include $(LEPTONICA_PATH)/../Android.mk
include $(TESSERACT_PATH)/../Android.mk
include $(IMAGE_PROCESSING_PATH)/../Android.mk
