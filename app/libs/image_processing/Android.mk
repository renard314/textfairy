LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)


LOCAL_MODULE := libimage_processing

IMAGE_PROCESSING_SRC_FILES := \
  $(wildcard $(IMAGE_PROCESSING_PATH)/src/binarize/*.cpp) \
  $(wildcard $(IMAGE_PROCESSING_PATH)/src/blur_detect/*.cpp) \
  $(wildcard $(IMAGE_PROCESSING_PATH)/src/dewarp/*.cpp) \
  $(wildcard $(IMAGE_PROCESSING_PATH)/src/edge_detect/*.cpp) \
  $(wildcard $(IMAGE_PROCESSING_PATH)/src/enhance/*.cpp) \
  $(wildcard $(IMAGE_PROCESSING_PATH)/src/pageseg/*.cpp) \
  $(wildcard $(IMAGE_PROCESSING_PATH)/src/skew/*.cpp) \
  $(wildcard $(IMAGE_PROCESSING_PATH)/src/text_stat/*.cpp) \
  $(wildcard $(IMAGE_PROCESSING_PATH)/src/*.cpp)



LOCAL_SRC_FILES := $(subst $(LOCAL_PATH)/,,$(IMAGE_PROCESSING_SRC_FILES))
LOCAL_SRC_FILES += $(IMAGE_PROCESSING_PATH)/../image_processing.cpp

LOCAL_C_INCLUDES += \
  $(IMAGE_PROCESSING_PATH)/../ \
  $(IMAGE_PROCESSING_PATH)/src \
  $(IMAGE_PROCESSING_PATH)/src/binarize \
  $(IMAGE_PROCESSING_PATH)/src/blur_detect \
  $(IMAGE_PROCESSING_PATH)/src/dewarp \
  $(IMAGE_PROCESSING_PATH)/src/skew \
  $(IMAGE_PROCESSING_PATH)/src/edge_detect \
  $(IMAGE_PROCESSING_PATH)/src/enhance \
  $(IMAGE_PROCESSING_PATH)/src/pageseg \
  $(IMAGE_PROCESSING_PATH)/src/text_stat

ifneq ("$(wildcard $(ADAPTIVE_BINARIZER_PATH)/PixBinarizer.cpp)","")
LOCAL_SRC_FILES += $(ADAPTIVE_BINARIZER_PATH)/PixBinarizer.cpp
LOCAL_SRC_FILES += $(ADAPTIVE_BINARIZER_PATH)/PixAdaptiveBinarizer.cpp
LOCAL_C_INCLUDES += $(ADAPTIVE_BINARIZER_PATH)
LOCAL_CFLAGS := -DHAS_ADAPTIVE_BINARIZER
endif

LOCAL_LDLIBS := -llog
  
#common
LOCAL_SHARED_LIBRARIES:= liblept
LOCAL_DISABLE_FORMAT_STRING_CHECKS:=true
include $(BUILD_SHARED_LIBRARY)