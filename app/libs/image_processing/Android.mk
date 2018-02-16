LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)


LOCAL_MODULE := libimage_processing

# jni
LOCAL_SRC_FILES += \
  $(IMAGE_PROCESSING_PATH)/src/binarize.cpp \
  $(IMAGE_PROCESSING_PATH)/src/pageseg.cpp \
  $(IMAGE_PROCESSING_PATH)/src/RunningStats.cpp \
  $(IMAGE_PROCESSING_PATH)/src/PixBlurDetect.cpp \
  $(IMAGE_PROCESSING_PATH)/src/image_processing_util.cpp \
  $(IMAGE_PROCESSING_PATH)/src/dewarp.cpp \
  $(IMAGE_PROCESSING_PATH)/src/TimerUtil.cpp \
  $(IMAGE_PROCESSING_PATH)/src/PixEdgeDetector.cpp \
  $(IMAGE_PROCESSING_PATH)/src/PixAdaptiveBinarizer.cpp \
  $(IMAGE_PROCESSING_PATH)/src/PixBinarizer.cpp \
  $(IMAGE_PROCESSING_PATH)/src/SkewCorrector.cpp \
  image_processing.cpp


LOCAL_C_INCLUDES += $(IMAGE_PROCESSING_PATH)/src

LOCAL_LDLIBS += \
  -llog \
  
#common
LOCAL_SHARED_LIBRARIES:= liblept
LOCAL_DISABLE_FORMAT_STRING_CHECKS:=true
include $(BUILD_SHARED_LIBRARY)