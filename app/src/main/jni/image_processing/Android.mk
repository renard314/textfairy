LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)


LOCAL_MODULE := libimage_processing_jni

# jni
LOCAL_SRC_FILES += \
  $(IMAGE_PROCESSING_PATH)/binarize.cpp \
  $(IMAGE_PROCESSING_PATH)/pageseg.cpp \
  $(IMAGE_PROCESSING_PATH)/RunningStats.cpp \
  $(IMAGE_PROCESSING_PATH)/PixBlurDetect.cpp \
  $(IMAGE_PROCESSING_PATH)/image_processing_util.cpp \
  $(IMAGE_PROCESSING_PATH)/dewarp.cpp \
  $(IMAGE_PROCESSING_PATH)/TimerUtil.cpp \
  $(IMAGE_PROCESSING_PATH)/PixEdgeDetector.cpp \
  $(IMAGE_PROCESSING_PATH)/PixAdaptiveBinarizer.cpp \
  $(IMAGE_PROCESSING_PATH)/PixBinarizer.cpp \
  $(IMAGE_PROCESSING_PATH)/SkewCorrector.cpp \
  image_processing.cpp


LOCAL_C_INCLUDES += $(IMAGE_PROCESSING_PATH)
LOCAL_C_INCLUDES += $(LEPTONICA_SRC_PATH)/src

LOCAL_LDLIBS += \
  -llog \
#  -lstdc++ \

LOCAL_LDFLAGS := -L$(TESS_TWO_PATH)/libs/$(TARGET_ARCH_ABI)/ -llept

  
#common
LOCAL_SHARED_LIBRARIES:= libjpeg
LOCAL_PRELINK_MODULE:= false
LOCAL_DISABLE_FORMAT_STRING_CHECKS:=true
include $(BUILD_SHARED_LIBRARY)