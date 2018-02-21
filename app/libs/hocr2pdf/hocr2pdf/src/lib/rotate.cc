/*
 * Copyright (C) 2006 - 2010 René Rebe, ExactCODE GmbH Germany.
 *           (C) 2006, 2007 Archivista GmbH, CH-8042 Zuerich
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

#include <math.h>

#include <iostream>
#include <iomanip>

#include "Image.hh"
#include "ImageIterator2.hh"
#include "../codecs/Codecs.hh"

#include "rotate.hh"

void flipX (Image& image)
{
  // thru the codec?
  if (!image.isModified() && image.getCodec())
    if (image.getCodec()->flipX(image))
      return;
  
  const int stride = image.stride();
  uint8_t* data = image.getRawData();
  switch (image.spp * image.bps)
    {
    case 1:
    case 2:
    case 4:
      {
	// create a reversed bit table for fast lookup
	uint8_t reversed_bits[256];
	
	const int bps = image.bps;
	const int mask = (1 << bps) - 1;
	
	for (int i = 0; i < 256; ++i) {
	  uint8_t rev = 0, v = i;
	  for (int j = 0; j < 8/bps; ++j) {
	    rev = (rev << bps) | (v & mask);
	    v >>= bps;
	  }
	  reversed_bits[i] = rev;
	}
	
	for (int y = 0; y < image.h; ++y)
	  {
	    uint8_t* row = &data [y*stride];
	    for (int x = 0; x < stride/2; ++x) {
	      uint8_t v = row [x];
	      row[x] = reversed_bits [row[stride - 1 - x]];
	      row[stride - 1 - x] = reversed_bits[v];
	    }
	  }
      }
      break;
    case 8:
    case 16:
    case 24:
    case 32:
    case 48:
      {
	const unsigned int bytes = image.spp * image.bps / 8;
	for (int y = 0; y < image.h; ++y)
	  {
	    uint8_t* ptr1 = &data[y * stride];
	    uint8_t* ptr2 = ptr1 + stride - bytes;
	    
	    for (; ptr1 < ptr2; ptr2 -= 2 * bytes) {
	      for (int b = 0; b < bytes; b++) {
		uint8_t v = *ptr1;
		*ptr1++ = *ptr2;
		*ptr2++ = v;
	      }
	    }
	  }
      }
      break;
    default:
      std::cerr << "flipX: unsupported depth." << std::endl;
      return;
    }
  image.setRawData();
}

void flipY (Image& image)
{
  // thru the codec?
  if (!image.isModified() && image.getCodec())
    if (image.getCodec()->flipY(image))
      return;
  
  const unsigned int bytes = image.stride();
  uint8_t* data = image.getRawData();
  for (int y = 0; y < image.h / 2; ++y)
    {
      int y2 = image.h - y - 1;

      uint8_t* row1 = &data[y * bytes];
      uint8_t* row2 = &data[y2 * bytes];

      for (int x = 0; x < bytes; ++x)
	{
	  uint8_t v = *row1;
	  *row1++ = *row2;
	  *row2++ = v;
	}
    }
  image.setRawData();
}

void rot90 (Image& image, int angle)
{
  bool cw = false; // clock-wise
  if (angle == 90)
    cw = true; // else 270 or -90 or whatever and thus counter cw
  
  int rot_stride = (image.h * image.spp * image.bps + 7) / 8;
   
  uint8_t* data = image.getRawData();
  uint8_t* rot_data = (uint8_t*) malloc(rot_stride * image.w);
  
  switch (image.spp * image.bps)
    {
    case 1:
    case 2:
    case 4: {
      const int bps = image.bps;
      const int spb = 8 / bps; // Samples Per Byte
      const uint8_t mask =  0xF00 >> bps;
      // std::cerr << "mask: " << (int)mask << std::endl;
      
      for (int y = 0; y < image.h; ++y) {
	uint8_t* new_row;
	if (cw)
	  new_row = &rot_data [ (image.h - 1 - y) / spb ];
	else
	  new_row = &rot_data [ (image.w - 1) * rot_stride + y / spb ];
	
	for (int x = 0; x < image.w;) {
	  // spread the bits thru the various row slots
	  uint8_t bits = *data++;
	  int i = 0;
	  for (; i < spb && x < image.w; ++i) {
	    if (cw) {
	      *new_row = *new_row >> bps | (bits & mask);
	      new_row += rot_stride;
	    }
	    else {
	      *new_row = *new_row << bps | (bits & mask) >> (8-bps);
	      new_row -= rot_stride;
	    }
	    bits <<= bps;
	    ++x;
	  }
	  // finally shift the last line if necessary
	  // TODO: recheck this residual bit for correctness
	  if (i < spb) {
	    if (cw) {
	      new_row -= rot_stride;
	      *new_row = *new_row >> (8 - (bps*i));
	    }
	    else {
	      new_row += rot_stride;
	      *new_row = *new_row << (8 - (bps*i));
	      
	    }
	    bits <<= 1;
	    ++x;
	  }
	}
      }
    }
      break;
      
    case 8:
    case 16:
    case 24:
    case 32:
    case 48:
      {
	const int bps = (image.bps + 7) / 8 * image.spp; // bytes...
	
	for (int y = 0; y < image.h; ++y) {
	  uint8_t* new_row =
	    cw ?
	    &rot_data[(image.h - 1 - y) * bps] :
	    &rot_data[(image.w - 1) * rot_stride + (y * bps)];
	  
	  for (int x = 0; x < image.w; ++x) {
	    for (int i = 0; i < bps; ++i)
	      *new_row++ = *data++;
	    new_row += cw ? rot_stride - bps : -rot_stride - bps;
	  }
	}
      }
      break;
      
    default:
      std::cerr << "rot90: unsupported depth. spp: "
		<< image.spp << ", bpp:" << image.bps << std::endl;
      free (rot_data);
      return;
    }
  
  // we are done, tweak the w/h
  int x = image.w;
  image.w = image.h;
  image.h = x;
  // resolution, likewise
  image.setResolution(image.resolutionY(), image.resolutionX());
  
  // set the new data
  image.setRawData (rot_data);
}

template <typename T>
struct rotate_template
{
  void operator() (Image& image, double angle, const Image::iterator& background)
  {
    angle = angle / 180 * M_PI;
  
    const int xcent = image.w / 2;
    const int ycent = image.h / 2;
  
    Image orig_image; orig_image.copyTransferOwnership(image);
    image.resize (image.w, image.h);

    const double cached_sin = sin (angle);
    const double cached_cos = cos (angle);
  
    #pragma omp parallel for schedule (dynamic, 16)
    for (int y = 0; y < image.h; ++y)
    {
      T it (image);
      it.at(0, y);
      for (int x = 0; x < image.w; ++x)
	{
	  double ox =   (x - xcent) * cached_cos + (y - ycent) * cached_sin;
	  double oy = - (x - xcent) * cached_sin + (y - ycent) * cached_cos;
	  
	  ox += xcent;
	  oy += ycent;
	  
	  typename T::accu a;
	  if (ox >= 0 && oy >= 0 &&
	      ox < image.w && oy < image.h)
	    {
	      int oxx = (int)floor(ox);
	      int oyy = (int)floor(oy);
	      
	      int oxx2 = std::min (oxx + 1, image.w - 1);
	      int oyy2 = std::min (oyy + 1, image.h - 1);
	      
	      int xdist = (int) ((ox - oxx) * 256);
	      int ydist = (int) ((oy - oyy) * 256);

	      T orig_it (orig_image);
	      a  = (*orig_it.at(oxx,  oyy))  * ((256 - xdist) * (256 - ydist));
	      a += (*orig_it.at(oxx2, oyy))  * (xdist         * (256 - ydist));
	      a += (*orig_it.at(oxx,  oyy2)) * ((256 - xdist) * ydist);
	      a += (*orig_it.at(oxx2, oyy2)) * (xdist         * ydist);
	      a /= (256 * 256);
	    }
	  else
	    a = (background);
	  
	  it.set (a);
	  ++it;
	}
    }
    image.setRawData ();
  }
};

void rotate (Image& image, double angle, const Image::iterator& background)
{
  angle = fmod (angle, 360);
  if (angle < 0)
    angle += 360;
      
  if (angle == 0.0)
    return;

  // thru the codec?
  if (!image.isModified() && image.getCodec())
    if (image.getCodec()->rotate(image, angle))
       return;
   
  if (angle == 180.0) {
    flipX (image);
    flipY (image);
    return;
  }

  if (angle == 90.0) {
    rot90 (image, 90);
    return;
  }
 
  if (angle == 270.0) {
    rot90 (image, 270);
    return;
  }

  codegen<rotate_template> (image, angle, background);
}

void exif_rotate(Image& image, unsigned exif_orientation)
{
  Image::iterator bgrd(image.begin()); // not used
  
  //std::cerr << exif_orientation << std::endl;
  switch (exif_orientation) {
  case 0: // undefined, but handled as NOP
  case 1: // top, left side
    break;
  case 2: // top, rigth side
    flipX(image); break;
  case 3: // bottom, rigth side
    rotate(image, 180, bgrd); break;
  case 4: // bottom, left side
    flipY(image); break;
  case 5: // left side, top
    rotate(image, -90, bgrd); break; // tested
  case 6: // right side, top
    rotate(image, 90, bgrd); break; // tested
  case 7: // right side, bottom
    rotate(image, 90, bgrd); flipX(image); break;
  case 8: // left side, bottom
    rotate(image, -90, bgrd); break; // tested
  default:
    std::cerr << "unknown exif orientation: " << exif_orientation << std::endl;
  }
}


template <typename T>
struct copy_crop_rotate_template
{
  Image* operator() (Image& image, int x_start, int y_start,
		     unsigned int w, unsigned int h,
		     double angle, const Image::iterator& background)
  {
    angle = fmod (angle, 360);
    if (angle < 0)
      angle += 360;
    
    // trivial code just for testing, to be optimized
    
    angle = angle / 180 * M_PI;
    
    Image* new_image = new Image;
    new_image->copyMeta (image);
    new_image->resize (w, h);
    
    const double cached_sin = sin (angle);
    const double cached_cos = cos (angle);

    #pragma omp parallel for schedule (dynamic, 16)
    for (unsigned int y = 0; y < h; ++y)
    {
      T it (*new_image);
      it.at(0, y);
      for (unsigned int x = 0; x < w; ++x)
	{
	  const double ox = ( (double)x * cached_cos + (double)y * cached_sin) + x_start;
	  const double oy = (-(double)x * cached_sin + (double)y * cached_cos) + y_start;

	  T orig_it (image);
	  typename T::accu a;
 
	  if (ox >= 0 && oy >= 0 &&
	      ox < image.w && oy < image.h) {
	    
	    int oxx = (int)floor(ox);
	    int oyy = (int)floor(oy);
	    
	    int oxx2 = std::min (oxx+1, image.w-1);
	    int oyy2 = std::min (oyy+1, image.h-1);
	    
	    int xdist = (int) ((ox - oxx) * 256);
	    int ydist = (int) ((oy - oyy) * 256);
	    
	    a  = (*orig_it.at(oxx,  oyy))  * ((256 - xdist) * (256 - ydist));
	    a += (*orig_it.at(oxx2, oyy))  * (xdist         * (256 - ydist));
	    a += (*orig_it.at(oxx,  oyy2)) * ((256 - xdist) * ydist);
	    a += (*orig_it.at(oxx2, oyy2)) * (xdist         * ydist);
	    a /= (256 * 256);
	  }
	  else
	    a = (background);
	  
	  it.set (a);
	  ++it;
	}
    }
    return new_image;
  }
};

Image* copy_crop_rotate (Image& image, int x_start, int y_start,
			 unsigned int w, unsigned int h,
			 double angle, const Image::iterator& background)
{
  return codegen_return<Image*, copy_crop_rotate_template> (image, x_start, y_start,
							    w, h, angle, background);
}
