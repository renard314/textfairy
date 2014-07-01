LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE:= libimage_processing

LOCAL_SRC_FILES := binarize.cpp pageseg.cpp util.cpp RunningTextlineStats.cpp text_search.cpp RunningStats.cpp

LOCAL_C_INCLUDES := $(LEPTONICA_PATH)/src


LOCAL_EXPORT_C_INCLUDES += $(LOCAL_PATH)

LOCAL_LDLIBS += \
  -llog \
  -lstdc++ \
 
LOCAL_PRELINK_MODULE := false  
LOCAL_SHARED_LIBRARIES := liblept libtess

include $(BUILD_SHARED_LIBRARY)
