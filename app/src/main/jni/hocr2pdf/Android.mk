LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := hocr2pdfjni

LOCAL_CPP_EXTENSION :=.cc

# jni
LOCAL_SRC_FILES := hocr2pdf.cc

LOCAL_C_INCLUDES += \
  $(LIBJPEG_PATH) \
  $(HOCR2PDF_PATH)/codecs \
  $(HOCR2PDF_PATH)/utility \
  $(HOCR2PDF_PATH)/lib


LOCAL_LDLIBS := \
  -llog \
  -lz

#common
LOCAL_SHARED_LIBRARIES:= libjpeg libhocr2pdf
LOCAL_PRELINK_MODULE:= false
LOCAL_DISABLE_FORMAT_STRING_CHECKS:=true

include $(BUILD_SHARED_LIBRARY)