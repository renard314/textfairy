/*
 * Colorspace conversions..
 * Copyright (C) 2006 - 2008 René Rebe, ExactCODE
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
 *
 * Alternatively, commercial licensing options are available from the
 * copyright holder ExactCODE GmbH Germany.
 */

#include <string.h> // memmove
#include <iostream>
#include <algorithm>

#include "Image.hh"
#include "../codecs/Codecs.hh"

#include "Colorspace.hh"

#include "crop.hh"

void crop (Image& image, unsigned int x, unsigned int y, unsigned int w, unsigned int h)
{
  // limit to valid boundaries
  x = std::min (x, (unsigned)image.w-1);
  y = std::min (y, (unsigned)image.h-1);
  
  w = std::min (w, (unsigned)image.w-x);
  h = std::min (h, (unsigned)image.h-y);

  // something to do?
  if (x == 0 && y == 0 && w == (unsigned int)image.w && h == (unsigned int)image.h)
    return;
  
  if (!image.isModified() && image.getCodec())
    if (image.getCodec()->crop(image, x, y, w, h))
      return;
  
  /*
    std::cerr << "after limiting: " << x << " " << y
    << " " << w << " " << h << std::endl;
  */

  // truncate the height, this is optimized for the "just height" case
  // (of e.g. fastAutoCrop)
  if (x == 0 && y == 0 && w == (unsigned int)image.w) {
    image.setRawData (); // invalidate
    image.h = h;
    return;
  }
  
  // bit shifting is too expensive, crop at least byte-wide
  int orig_bps = image.bps;
  if (orig_bps < 8)
    colorspace_grayX_to_gray8 (image);
  
  int stride = image.stride();
  int cut_stride = stride * w / image.w;
  
  uint8_t* dst = image.getRawData ();
  uint8_t* src = dst + stride * y + (stride * x / image.w);
  
  for (unsigned int i = 0; i < h; ++i) {
    memmove (dst, src, cut_stride);
    dst += cut_stride;
    src += stride;
  }
  
  image.setRawData (); // invalidate
  image.w = w;
  image.h = h;
 
  switch (orig_bps) {
  case 1:
    colorspace_gray8_to_gray1 (image);
    break;
  case 2:
    colorspace_gray8_to_gray2 (image);
    break;
  case 4:
    colorspace_gray8_to_gray4 (image);
    break;
  default:
    ;
  }
}

// auto crop just the bottom of an image filled in the same, solid color
// optimization: for sub-byte depth we compare a 8bit pattern unit at-a-time
void fastAutoCrop (Image& image)
{
  if (!image.getRawData())
    return;
  
  const int stride = image.stride();
  const unsigned int bytes = (image.spp * image.bps + 7) / 8;
  
  int h = image.h - 1;
  uint8_t* data = image.getRawData() + stride * h;
  
  // which value to compare against, first pixel of the last line
  uint8_t v[bytes];
  memcpy(v, data, bytes);
  
  for (; h >= 0; --h, data -= stride) {
    // data row
    int i = 0;
    for (; i < stride; i += bytes)
      {
	if (data[i] != v[0] ||
	    (bytes > 1 && memcmp(&data[i+1], &v[1], bytes - 1) != 0)) {
	  break; // pixel differs, break out
	}
      }
    
    if (i != stride)
      break; // non-solid line, break out
  }
  ++h; // we are at the line that differs
  if (h == 0) // do not crop if the image is totally empty
    return;
  
  // We could just tweak the image height here, but using the generic
  // code we benefit from possible optimization, such as lossless
  // jpeg cropping.
  // We do not explicitly check if we crop, the crop function will optimize
  // a NOP crop away for all callers.
  return crop (image, 0, 0, image.w, h);
}
