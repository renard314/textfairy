LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libocr


# jni
LOCAL_SRC_FILES := ocr.cpp


LOCAL_LDLIBS := -llog
  
#common
LOCAL_SHARED_LIBRARIES:= libtess liblept libimage_processing
LOCAL_PRELINK_MODULE:= false

include $(BUILD_SHARED_LIBRARY)