MY_DIR := $(CURDIR)

HOCR2PDF_PATH := $(MY_DIR)/../hocr2pdf/src
IMAGE_PROCESSING_PATH := $(MY_DIR)/../OCRTest/src
TESSERACT_PATH := $(MY_DIR)/../tesseract-ocr-read-only
TESSERACT_CHANGE_PATH := $(MY_DIR)/../tesseract-changeset
LEPTONICA_PATH := $(MY_DIR)/../leptonica-1.68
LIBJPEG_PATH := $(MY_DIR)/../libjpeg


ifeq "$(HOCR2PDF_PATH)" ""
  $(error You must set the HOCR2PDF_PATH variable to the hocr2pdf source \
          directory. See README and jni/Android.mk for details)
endif

ifeq "$(LIBJPEG_PATH)" ""
  $(error You must set the LIBJPEG_PATH variable to the Android JPEG \
          source directory. See README and jni/Android.mk for details)
endif

ifeq "$(LEPTONICA_PATH)" ""
  $(error You must set the LEPTONICA_PATH variable to the leptonica \
          source directory. See README and jni/Android.mk for details)
endif

ifeq "$(TESSERACT_PATH)" ""
  $(error You must set the TESSERACT_PATH variable to the tesseract \
          source directory. See README and jni/Android.mk for details)
endif
# Just build the Android.mk files in the subdirs
include $(call all-subdir-makefiles) $(LIBJPEG_PATH)/Android.mk $(IMAGE_PROCESSING_PATH)/Android.mk $(HOCR2PDF_PATH)/Android.mk
