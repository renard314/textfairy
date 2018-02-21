/* The Plain Old Data encapsulation of pixel, raster data.
 * Copyright (C) 2005 - 2009 René Rebe
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2. A copy of the GNU General
 * Public License can be found in the file LICENSE.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANT-
 * ABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 */

/* Only minimal abstraction is done here to allow all sorts of
 * hand optimized low-level optimizations.
 *
 * On loading a codec might be attached. The codec might be querried
 * to decode the image data later on (decode on access) to allow
 * avoiding image decoding if the data is not accessed at all.
 *
 * Equivalently writing can be optimized by keeping the codec
 * around and saving the original data without recompression
 * (JPEG).
 *
 * Some methods in the codec allow working on the compressed data such
 * as orthogonal rotation, down-scaling, and cropping (e.g. of JPEG
 * DCT coefficients - like jpegtran, epeg).
 *
 * Call sequence on Read/Wrie:
 * to immediatly attach the data
 *   Image::New(w,h)
 *   Image::getRawData() // to write the data
 *   Image::setCodec()
 *
 * or to get on-demand decoding:
 *   set meta data (e.g. ::w, ::h, ::xdpi, ::ydpi, ...)
 *   Image::setRawData(0)
 *   Image::setCodec()
 *
 * Note: setCodec must be last as it marks the data as unmodifed.
 *
 * On access the data might be loaded on-demand:
 *   Image::getRawData()
 *     if !data then if codec then codec->decode() end end
 *
 * After modifing the POD image setRawData*() must be called to
 * notify about the update:
 *   Image::setRawData*()
 *     if !modified then codec->free() modified=true end 
 *
 * Again: If you modify more than meta data you must call:
 *   Image::setRawData()
 * even with the current data pointer remains equal to ensure
 * proper invalidation of the cached compressed codec data!
 *
 * Call sequence of the Codec's::encode*() if data is just rewritten:
 *     if image->isModified() then
 *       encode_new_data()
 *     else
 *       just copy existing compressed data (e.g. DCT)
 *     end
 *
 * The operator= create a complete clone of the image, the image
 * buffers are not shared (anymore, formerly ownership was passed and
 * we had a seperate Clone() method). The attached codec is not
 * copied.
 */

#ifndef IMAGE_HH
#define IMAGE_HH

#include <inttypes.h>
#include <string>
#include <math.h> // for floor

#include <iostream>

// just forward
class ImageCodec;

/// temp. state to migrate away from public member's
#define DEPRECATED
#ifndef DEPRECATED
#ifdef __GNUC__
#define DEPRECATED __attribute__ ((deprecated))
#endif
#endif

#define WARN_UNHANDLED std::cerr << "unhandled spp/bps in " << __FILE__ << ":" << __LINE__ << std::endl


class Image
{
protected:
  bool modified, meta_modified;
  int xres, yres;
  
  std::string decoderID;
  ImageCodec* codec;
  
  uint8_t* data;

public:
  
  uint8_t* getRawData () const;
  uint8_t* getRawDataEnd () const;

  void setRawData (); // just mark modified
  void setRawData (uint8_t* _data);
  void setRawDataWithoutDelete (uint8_t* _data);
  
  void resize (int _w, int _h);
  void New (int _w, int _h) DEPRECATED { resize (_w, _h); }
  
  void setDecoderID (const std::string& id);
  const std::string& getDecoderID ();
  ImageCodec* getCodec();
  void setCodec (ImageCodec* _codec);
  
  bool isModified () { return modified; }
  bool isMetaModified () { return meta_modified; }
  
  typedef enum {
    GRAY1 = 1,
    GRAY2,
    GRAY4,
    GRAY8,
    //    GRAY8A,
    GRAY16,
    //    GRAY16A,
    RGB8,
    RGB8A,
    RGB16,
    //    RGB16A,
    CMYK8,
    //    CMYK16,
    YUV8,
    // YUVK8 - really appears in the wild? JPEG appears to support this (Y/Cb/Cr/K)
  } type_t;

  typedef union {
    uint8_t gray;
    uint16_t gray16;
    struct {
      uint8_t r;
      uint8_t g;
      uint8_t b;
    } rgb;
    struct {
      uint8_t r;
      uint8_t g;
      uint8_t b;
      uint8_t a;
    } rgba;
    struct {
      uint16_t r;
      uint16_t g;
      uint16_t b;
    } rgb16;
    struct {
      uint8_t c;
      uint8_t m;
      uint8_t y;
      uint8_t k;
    } cmyk;
    struct {
      uint8_t y;
      uint8_t u;
      uint8_t v;
    } yuv;
  } value_t;
    
  typedef union {
    int32_t gray;
    struct {
      int32_t r;
      int32_t g;
      int32_t b;
    } rgb;
    struct {
      int32_t r;
      int32_t g;
      int32_t b;
      int32_t a;
    } rgba;
    struct {
      int32_t c;
      int32_t m;
      int32_t y;
      int32_t k;
    } cmyk;
    struct {
      int32_t y;
      int32_t u;
      int32_t v;
    } yuv;
  } ivalue_t;

  int width () const { return w; }
  int height () const { return h; }
  
  int stride () const {
    return (w * spp * bps + 7) / 8;
  }
  
  int Stride () const DEPRECATED {
    return stride ();
  }
  
  int bitsPerSample () const { return bps; }
  int bitsPerPixel () const { return bps * spp; }
  int samplesPerPixel () const { return spp; }

  int resolutionX () const { return xres; }
  int resolutionY () const { return yres; }

  void setWidth (int _w) { w = _w; }
  void setHeight (int _h) { h = _h; }

  void setBitsPerSample (int _bps) { bps = _bps; }
  void setSamplesPerPixel (int _spp) { spp = _spp; }

  void setResolution (int _xres, int _yres) {
    if (xres != _xres || yres != _yres)
      meta_modified = true;
    xres = _xres;
    yres = _yres;
  }
  void setResolutionX (int _xres) { setResolution(_xres, yres); }
  void setResolutionY (int _yres) { setResolution(xres, _yres); }

  /* TODO: should be unsigned */
  int w DEPRECATED, h DEPRECATED, bps DEPRECATED, spp DEPRECATED;
  
public:
  
  Image ();
  Image (Image& other);
  ~Image ();
  
  
  Image& operator= (const Image& other);
  void copyTransferOwnership (Image& other);
  
  type_t Type () const {
    switch (spp*bps) {
    case 1:  return GRAY1;
    case 2:  return GRAY2;
    case 4:  return GRAY4;
    case 8:  return GRAY8;
    case 16: return GRAY16;
    case 24: return RGB8;
    case 32: return RGB8A;
    case 48: return RGB16;
    default:
      WARN_UNHANDLED;
      return (Image::type_t)0;
    }
  }

#define CONST const
#include "ImageIterator.hh"
#include "ImageIterator.hh"

  const_iterator begin () const {
    return const_iterator (this, false);
  }

  const_iterator end () const {
    return const_iterator (this, true);
  }

  iterator begin () {
    return iterator (this, false);
  }
  
  iterator end () {
    return iterator (this, true);
  }

  void copyMeta (const Image& other);
  
protected:
};

typedef struct { uint8_t r, g, b; } rgb;
typedef struct { uint8_t r, g, b, a;} rgba;
typedef struct { uint16_t r, g, b; } rgb16;

#undef WARN_UNHANDLED
#undef DEPRECATED
#endif
