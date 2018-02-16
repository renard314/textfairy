/*
 * Colorspace conversions.
 * Copyright (C) 2006 - 2010 René Rebe, ExactCOD GmbH Germany
 * Copyright (C) 2007 Susanne Klaus, ExactCODE
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

#include <iostream>
#include <map>

#include "Image.hh"
#include "ImageIterator2.hh"

#include "../codecs/Codecs.hh"
#include "Colorspace.hh"

#include "../utility/Endianess.hh"

template <typename T>
struct normalize_template
{
  void operator() (Image& image, uint8_t l, uint8_t h)
  {
    typename T::accu a;
    typename T::accu::vtype black, white;
    
    // darkest 1%, lightest .5%
    const int white_point = image.w * image.h / 100;
    const int black_point = white_point / 2;
    
    {
      // TODO: fall back to map for HDR types
      typedef std::vector<typename T::accu::vtype> histogram_type;
      typename T::accu::vtype hsize;
      T::accu::one().getL(hsize);
      histogram_type histogram(hsize);
      
      T it (image);
      typename T::accu::vtype l;
      for (int y = 0; y < image.h; ++y) {
	for (int x = 0; x < image.w; ++x)
	  {
	    a = *it;
	    a.getL(l); // TODO: create discrete interval for floats etc.
	    histogram[l]++;
	    ++it;
	  }
      }

      // find suitable black and white points
      int count = 0, ii = 0;
      for (typename histogram_type::iterator i = histogram.begin();
	   i != histogram.end(); ++i, ++ii) {
	count += *i;
	if (count >= black_point) {
	  black = ii;
	  break;
	}
      }
      count = 0; ii = 255;
      for (typename histogram_type::reverse_iterator i = histogram.rbegin();
	   i != histogram.rend(); ++i, --ii) {
	count += *i;
	if (count >= white_point) {
	  white = ii;
	  break;
	}
      }
    }
    
    // TODO: scale to type range
    if (l)
      black = l;
    if (h)
      white = h;
    
    typename T::accu::vtype fa, fb = -black;
    T::accu::one().getL(fa);
    fa *= 256; // shift for interger multiplication
    fa /= (white - black);
    
    T it (image);
    for (int y = 0; y < image.h; ++y) {
      for (int x = 0; x < image.w; ++x)
	{
	  a = *it;
	  a += fb;
	  a *= fa;
	  a /= 256;
	  a.saturate();
	  it.set (a);
	  ++it;
	}
    }
    image.setRawData();
  }
};

void normalize (Image& image, uint8_t l, uint8_t h)
{
  codegen<normalize_template> (image, l, h);
}

void colorspace_rgba8_to_rgb8 (Image& image)
{
  uint8_t* output = image.getRawData();
  for (uint8_t* it = image.getRawData(); it < image.getRawData() + image.w*image.h*image.spp;)
    {
      *output++ = *it++;
      *output++ = *it++;
      *output++ = *it++;
      it++; // skip over a
    }
  image.spp = 3; // converted data right now
  image.setRawData();
}


void colorspace_rgb8_to_gray8 (Image& image, const int bytes)
{
  uint8_t* output = image.getRawData();
  for (uint8_t* it = image.getRawData(); it < image.getRawData() + image.stride() * image.h; it += bytes)
    {
      // R G B order and associated weighting
      int c = (int)it[0] * 28;
      c += (int)it[1] * 59;
      c += (int)it[2] * 11;
      *output++ = (uint8_t)(c / 100);
    }
  image.spp = 1; // converted data right now
  image.setRawData();
}

void colorspace_rgb16_to_gray16 (Image& image)
{
  uint16_t* output = (uint16_t*)image.getRawData();
  for (uint16_t* it = output;
       it < (uint16_t*)(image.getRawData() + image.stride() * image.h);)
    {
      // R G B order and associated weighting
      int c = (int)*it++ * 28;
      c += (int)*it++ * 59;
      c += (int)*it++ * 11;
      
      *output++ = (uint16_t)(c / 100);
    }
  image.spp = 1; // converted data right now
  image.setRawData();
}

void colorspace_rgb8_to_rgb8a (Image& image, uint8_t alpha)
{
  image.setRawDataWithoutDelete	((uint8_t*)realloc(image.getRawData(),
						   image.w * 4 * image.h));
  image.setSamplesPerPixel(4);
  
  // reverse copy with alpha fill inside the buffer
  uint8_t* it_src = image.getRawData() + image.w * 3 * image.h - 1;
  for (uint8_t* it_dst = image.getRawDataEnd() - 1; it_dst > image.getRawData();)
    {
      *it_dst-- = alpha;
      *it_dst-- = *it_src--;
      *it_dst-- = *it_src--;
      *it_dst-- = *it_src--;
    }
}

void colorspace_gray8_threshold (Image& image, uint8_t threshold)
{
  for (uint8_t* it = image.getRawData(); it < image.getRawDataEnd(); ++it)
    *it = *it > threshold ? 0xFF : 0x00;
  
  image.setRawData();
}

void colorspace_gray8_denoise_neighbours (Image &image, bool gross)
{
  if (image.bps != 8 || image.spp != 1)
    return;
  
  uint8_t* it = image.getRawData();
  uint8_t* ndata = (uint8_t*)malloc(image.stride() * image.h);
  uint8_t* it2 = ndata;
  
  struct compare_and_set
  {
    const Image& image;
    const unsigned int stride;
    compare_and_set (const Image& _image)
      : image(_image), stride (image.stride())
    {
    }
    
    // without the inner(area) compiler guidance the conditionals are
    // not optimized away well enough
    void operator() (const int x, const int y, uint8_t* it, uint8_t* it2,
		     const bool inner, const bool gross = false)
    {
      int n = 0, sum = 0;
      
      // +
      if (inner || x > 0)
	sum += it[-1], ++n;
      if (inner || y > 0)
	sum += it[-stride], ++n;
      
      if (inner || x < image.w-1)
	sum += it[1], ++n;
      if (inner || y < image.h-1)
	sum += it[stride], ++n;

      // x
      if (gross) {
	if (inner || y > 0) {
	  if (inner || x > 0)
	    sum += it[-stride - 1], ++n;
	  if (inner || x < image.w-1)
	    sum += it[-stride + 1], ++n;
	}
	
	if (inner || y < image.h-1) {
	  if (inner || x > 0)
	    sum += it[stride - 1], ++n;
	  if (inner || x < image.w-1)
	    sum += it[stride + 1], ++n;
	}
      }
      
      // if all direct neighbours are black or white, fill it
      if (gross) {
        if (sum <= 1 * 0xff)
	  *it2 = 0;
        else if (sum >= (n - 1) * 0xff)
	  *it2 = 0xff;
	else
	  *it2 = *it;
      } else {
        if (sum == 0)
	  *it2 = 0;
        else if (sum == n * 0xff)
	  *it2 = 0xff;
	else
	  *it2 = *it;
      }
    }
  } compare_and_set (image);
  
  for (int y = 0; y < image.h; ++y)
    {
      // optimize conditionals away for the inner area
      if (y > 0 && y < image.h-1)
	{
	  compare_and_set (0, y, it++, it2++, false, gross);
	  for (int x = 1; x < image.w-1; ++x)
	    compare_and_set (x, y, it++, it2++, true, gross);
	  compare_and_set (image.w-1, y, it++, it2++, false, gross);
	}
      else // quite some out of bounds conditions to check
	for (int x = 0; x < image.w; ++x) 
	  compare_and_set (x, y, it++, it2++, false, gross);
    }
    
  image.setRawData(ndata);
}

void colorspace_gray8_to_gray1 (Image& image, uint8_t threshold)
{
  uint8_t *output = image.getRawData();
  uint8_t *input = image.getRawData();
  
  for (int row = 0; row < image.h; row++)
    {
      uint8_t z = 0;
      int x = 0;
      for (; x < image.w; x++)
	{
	  z <<= 1;
	  if (*input++ > threshold)
	    z |= 0x01;
	  
	  if (x % 8 == 7)
	    {
	      *output++ = z;
	      z = 0;
	    }
	}
      int remainder = 8 - x % 8;
      if (remainder != 8)
	{
	  z <<= remainder;
	  *output++ = z;
	}
    }
  image.bps = 1;
  image.setRawData();
}

void colorspace_gray8_to_gray4 (Image& image)
{
  uint8_t *output = image.getRawData();
  uint8_t *input = image.getRawData();
  
  for (int row = 0; row < image.h; row++)
    {
      uint8_t z = 0;
      int x = 0;
      for (; x < image.w; x++)
	{
	  z <<= 4;
	  z |= (*input++ >> 4) & 0xF;
	  
	  if (x % 2 == 1)
	    {
	      *output++ = z;
	      z = 0;
	    }
	}
      int remainder = 2 - x % 2;
      if (remainder != 2)
	{
	  z <<= 4*remainder;
	  *output++ = z;
	}
    }
  image.bps = 4;
  image.setRawData();
}
void colorspace_gray8_to_gray2 (Image& image)
{
  uint8_t *output = image.getRawData();
  uint8_t *input = image.getRawData();
  
  for (int row = 0; row < image.h; ++row)
    {
      uint8_t z = 0;
      int x = 0;
      for (; x < image.w; x++)
	{
	  z <<= 2;
	  z |= (*input++ >> 6) & 0x3;
	  
	  if (x % 4 == 3)
	    {
	      *output++ = z;
	      z = 0;
	    }
	}
      int remainder = 4 - x % 4;
      if (remainder != 4)
	{
	  z <<= 2*remainder;
	  *output++ = z;
	}
    }
  image.bps = 2;
  image.setRawData();
}

void colorspace_gray8_to_rgb8 (Image& image)
{
  uint8_t* data = (uint8_t*)malloc (image.w*image.h*3);
  uint8_t* output = data;
  for (uint8_t* it = image.getRawData ();
       it < image.getRawData() + image.w*image.h*image.spp; ++it)
    {
      *output++ = *it;
      *output++ = *it;
      *output++ = *it;
    }
  image.spp = 3; // converted data right now
  image.setRawData(data);
}

void colorspace_grayX_to_gray8 (Image& image)
{
  uint8_t* old_data = image.getRawData();
  int old_stride = image.stride();
  
  const int bps = image.bps;
  image.bps = 8;
  image.setRawDataWithoutDelete ((uint8_t*)malloc(image.h * image.stride()));
  uint8_t* output = image.getRawData();
  
  const int vmax = 1 << bps;
#ifdef _MSC_VER
  std::vector<uint8_t> gray_lookup(vmax);
#else
  uint8_t gray_lookup[vmax];
#endif
  for (int i = 0; i < vmax; ++i) {
    gray_lookup[i] = 0xff * i / (vmax - 1);
    //std::cerr << i << " = " << (int)gray_lookup[i] << std::endl;
  }
  
  const unsigned int bitshift = 8 - bps;
  for (int row = 0; row < image.h; ++row)
    {
      uint8_t* input = old_data + row * old_stride;
      uint8_t z = 0;
      unsigned int bits = 0;
      
      for (int x = 0; x < image.w; ++x)
	{
	  if (bits == 0) {
	    z = *input++;
	    bits = 8;
	  }
	  
	  *output++ = gray_lookup[z >> bitshift];
	  
	  z <<= bps;
	  bits -= bps;
	}
    }
  free (old_data);
}

void colorspace_grayX_to_rgb8 (Image& image)
{
  uint8_t* old_data = image.getRawData();
  int old_stride = image.stride();
  
  const int bps = image.bps;
  image.bps = 8;
  image.spp = 3;
  image.setRawDataWithoutDelete ((uint8_t*)malloc(image.h * image.stride()));
  uint8_t* output = image.getRawData();
  
  const int vmax = 1 << bps;
#ifdef _MSC_VER
  std::vector<uint8_t> gray_lookup(vmax);
#else
  uint8_t gray_lookup[vmax];
#endif
  for (int i = 0; i < vmax; ++i) {
    gray_lookup[i] = 0xff * i / (vmax - 1);
    //std::cerr << i << " = " << (int)gray_lookup[i] << std::endl;
  }
  
  const unsigned int bitshift = 8 - bps;
  for (int row = 0; row < image.h; ++row)
    {
      uint8_t* input = old_data + row * old_stride;
      uint8_t z = 0;
      unsigned int bits = 0;
      
      for (int x = 0; x < image.w; ++x)
	{
	  if (bits == 0) {
	    z = *input++;
	    bits = 8;
	  }
	  
	  *output++ = gray_lookup[z >> bitshift];
	  *output++ = gray_lookup[z >> bitshift];
	  *output++ = gray_lookup[z >> bitshift];
	  
	  z <<= bps;
	  bits -= bps;
	}
    }
  free (old_data);
}

void colorspace_gray1_to_gray2 (Image& image)
{
  uint8_t* old_data = image.getRawData();
  int old_stride = image.stride();
  
  image.bps = 2;
  image.setRawDataWithoutDelete ((uint8_t*) malloc (image.h*image.stride()));
  uint8_t* output = image.getRawData();
  
  for (int row = 0; row < image.h; ++row)
    {
      uint8_t z = 0;
      uint8_t zz = 0;
      uint8_t* input = old_data + row * old_stride;

      int x;
      for (x = 0; x < image.w; ++x)
	{
	  if (x % 8 == 0)
	    z = *input++;

	  zz <<= 2;
	  if (z >> 7)
	    zz |= 0x3;
	  z <<= 1;
	  
	  if (x % 4 == 3)
	    *output++ = zz;
	}
      
      int remainder = 4 - x % 4;
      if (remainder != 4)
	{
	  zz <<= 2*remainder;
	  *output++ = zz;
	}
    }
  free (old_data);
}

void colorspace_gray1_to_gray4 (Image& image)
{
  uint8_t* old_data = image.getRawData();
  int old_stride = image.stride();
  
  image.bps = 4;
  image.setRawDataWithoutDelete ((uint8_t*) malloc (image.h*image.stride()));
  uint8_t* output = image.getRawData();
  
  for (int row = 0; row < image.h; ++row)
    {
      uint8_t z = 0;
      uint8_t zz = 0;
      
      uint8_t* input = old_data + row * old_stride;
      
      int x;
      for (x = 0; x < image.w; ++x)
	{
	  if (x % 8 == 0)
	    z = *input++;
	  
	  zz <<= 4;
	  if (z >> 7)
	    zz |= 0x0F;
	  z <<= 1;
	  
	  if (x % 2 == 1)
	    *output++ = zz;
	}
      
      int remainder = 2 - x % 2;
      if (remainder != 2)
	{
	  zz <<= 4*remainder;
	  *output++ = zz;
	}
    }
  
  free (old_data);
}

void colorspace_16_to_8 (Image& image)
{
  uint8_t* output = image.getRawData();
  for (uint8_t* it = output; it < image.getRawDataEnd(); it += 2)
    {
      if (Exact::NativeEndianTraits::IsBigendian)
	*output++ = it[0];
      else
	*output++ = it[1];
    }
  image.bps = 8; // converted 8bit data

  // reallocate, to free half of the memory
  image.setRawDataWithoutDelete	((uint8_t*)realloc(image.getRawData(),
		    		  image.stride() * image.h));
}

void colorspace_8_to_16 (Image& image)
{
  image.setRawDataWithoutDelete	((uint8_t*)realloc(image.getRawData(),
		    		 image.stride() * 2 * image.h));
	
  uint8_t* data = image.getRawData();
  uint16_t* data16 = (uint16_t*) data;
  
  for (signed int i = image.stride() * image.h - 1; i >= 0; --i)
    {
      data16[i] = data[i] * 0xffff / 255;
    }
  
  image.bps = 16; // converted 16bit data
}

void colorspace_de_palette (Image& image, int table_entries,
			    uint16_t* rmap, uint16_t* gmap, uint16_t* bmap)
{
  // detect 1bps b/w tables
  if (image.bps == 1 && table_entries >= 2) {
    if (rmap[0] == 0 &&
	gmap[0] == 0 &&
	bmap[0] == 0 &&
	rmap[1] >= 0xff00 &&
	gmap[1] >= 0xff00 &&
	bmap[1] >= 0xff00)
      {
	//std::cerr << "correct b/w table." << std::endl;
	return;
      }
    if (rmap[1] == 0 &&
	gmap[1] == 0 &&
	bmap[1] == 0 &&
	rmap[0] >= 0xff00 &&
	gmap[0] >= 0xff00 &&
	bmap[0] >= 0xff00)
      {
	//std::cerr << "inverted b/w table." << std::endl;
	for (uint8_t* it = image.getRawData();
	     it < image.getRawDataEnd();
	     ++it)
	  *it ^= 0xff;
	image.setRawData ();
	return;
      }
  }
  
  // detect gray tables
  bool is_gray = false;
  if (table_entries > 1) {
    bool is_ordered_gray = (image.bps == 8 || image.bps == 4 ||
			    image.bps == 2) && (1 << image.bps == table_entries);
    is_gray = true;
    
    // std::cerr << (1 << image.bps) << " vs " << table_entries << std::endl;
    
    // std::cerr << "checking for gray table" << std::endl;
    for (int i = 0; (is_gray || is_ordered_gray) && i < table_entries; ++i) {
      // std::cerr << rmap[i] << " " << gmap[i] << " " << bmap[i] << std::endl;
      if (rmap[i] >> 8 != gmap[i] >> 8 ||
	  rmap[i] >> 8 != bmap[i] >> 8) {
	is_gray = is_ordered_gray = false;
      }
      else if (is_ordered_gray) {
	const int ref = i * 0xff / (table_entries - 1);
	if (rmap[i] >> 8 != ref ||
	    gmap[i] >> 8 != ref ||
	    bmap[i] >> 8 != ref)
	  is_ordered_gray = false;
      }
    }
    
    // std::cerr << "gray: " << is_gray << ", is ordered: " << is_ordered_gray << std::endl;
    
    if (is_ordered_gray)
      return;
  }
  
  int new_size = image.w * image.h;
  if (!is_gray) // RGB
    new_size *= 3;
  
  uint8_t* orig_data = image.getRawData();
  uint8_t* new_data = (uint8_t*) malloc (new_size);
  
  uint8_t* src = orig_data;
  uint8_t* dst = new_data;

  // TODO: allow 16bit output if the palette contains that much dynamic

  const unsigned int bitshift = 8 - image.bps;
  while (dst < new_data + new_size)
  {
    uint8_t z = 0;
    unsigned int bits = 0;

    for (int x = 0; x < image.w; ++x)
    {
      if (bits == 0) {
        z = *src++;
        bits = 8;
      }

      if (is_gray) {
	*dst++ = rmap[z >> bitshift] >> 8;
      } else {
	*dst++ = rmap[z >> bitshift] >> 8;
	*dst++ = gmap[z >> bitshift] >> 8;
	*dst++ = bmap[z >> bitshift] >> 8;
      }

      z <<= image.bps;
      bits -= image.bps;
    }
  }

  image.bps = 8;
  if (is_gray)
    image.spp = 1;
  else
    image.spp = 3;

  image.setRawData (new_data);  
}

/*
bool colorspace_by_name (Image& image, const std::string& target_colorspace,
			 uint8_t threshold)
{
  std::string space = target_colorspace;
  std::transform (space.begin(), space.end(), space.begin(), tolower);
    
  int spp, bps;
  if (space == "bw" || space == "bilevel" || space == "gray1") {
    spp = 1; bps = 1;
  } else if (space == "gray2") {
    spp = 1; bps = 2;
  } else if (space == "gray4") {
    spp = 1; bps = 4;
  } else if (space == "gray" || space == "gray8") {
    spp = 1; bps = 8;
  } else if (space == "gray16") {
    spp = 1; bps = 16;
  } else if (space == "rgb" || space == "rgb8") {
    spp = 3; bps = 8;
  } else if (space == "rgba" || space == "rgba8") {
    spp = 4; bps = 8;
  } else if (space == "rgb16") {
    spp = 3; bps = 16;
  // TODO: CYMK, YVU, RGBA, GRAYA...
  } else {
    std::cerr << "Requested colorspace conversion not yet implemented."
              << std::endl;
    return false;
  }

  return colorspace_convert(image, spp, bps, threshold);
}
*/

bool colorspace_convert(Image& image, int spp, int bps, uint8_t threshold)
{
  // thru the codec?
  if (!image.isModified() && image.getCodec())
    if (spp == 1 && bps >= 8)
      if (image.getCodec()->toGray(image))
	return true;

  // no image data, e.g. for loading raw images
  if (!image.getRawData()) {
    image.spp = spp;
    image.bps = bps;
    return true;
  }
  
  // up
  if (image.bps == 1 && bps == 2)
    colorspace_gray1_to_gray2 (image);
  else if (image.bps == 1 && bps == 4)
    colorspace_gray1_to_gray4 (image);
  else if (image.bps < 8 && bps >= 8)
    colorspace_grayX_to_gray8 (image);
  
  // upscale to 8 bit even for sub byte gray since we have no inter sub conv., yet
  if (image.bps < 8 && image.bps > bps)
    colorspace_grayX_to_gray8 (image);
  
  if (image.bps == 8 && image.spp == 1 && spp == 3)
    colorspace_gray8_to_rgb8 (image);

  if (image.bps == 8 && bps == 16)
    colorspace_8_to_16 (image);
  
  // down
  if (image.bps == 16 && bps < 16)
    colorspace_16_to_8 (image);
  
  if (image.spp == 4 && spp < 4 && image.bps == 8) { // TODO: might be RGB16
    if (spp < 3)
      colorspace_rgb8_to_gray8 (image, 4);
    else
      colorspace_rgba8_to_rgb8 (image);
  }
  
  if (image.spp == 3 && spp == 1) {
    if (image.bps == 8) 
      colorspace_rgb8_to_gray8 (image);
    else if (image.bps == 16)
      colorspace_rgb16_to_gray16 (image);
  }
  
  if (spp == 1 && image.bps > bps) {
    if (image.bps == 8 && bps == 1)
      colorspace_gray8_to_gray1 (image, threshold);
    else if (image.bps == 8 && bps == 2)
      colorspace_gray8_to_gray2 (image);
    else if (image.bps == 8 && bps == 4)
      colorspace_gray8_to_gray4 (image);
  }
  
  if (image.spp != spp || image.bps != bps) {
    std::cerr << "Incomplete colorspace conversion. Requested: spp: "
              << spp << ", bps: " << bps
              << " - now at spp: " << image.spp << ", bps: " << image.bps
              << std::endl;
    return false;
  }
  return true;
}

const char* colorspace_name (Image& image)
{
  switch (image.spp * image.bps)
    {
    case 1: return "gray1";
    case 2: return "gray2";
    case 4: return "gray4";
    case 8: return "gray8";
    case 16: return "gray16";
    case 24: return "rgb8";
    case 32: return "rgba8";
    case 48: return "rgb16";
    default: return "";
    }
}

// --
#include <math.h>

static inline double getExponentContrast (double contrast) {
  return (contrast < 0.0) ? 1.0 + contrast : ( (contrast == 1.0) ? 127 : 1.0 / (1.0 - contrast) );
}

static inline double getExponentGamma (double gamma) {
  return 1.0 / gamma;
}

static inline double convert (double val,
			      double brightness,
			      double contrast,
			      double gamma)
{
    /* apply brightness */
    if (brightness < 0.0)
        val = val * (1.0 + brightness);
    else if (brightness > 0.0)
        val = val + ((1.0 - val) * brightness);

    /* apply contrast */
    if (contrast != 0.0) {
      double nvalue = (val > 0.5) ? 1.0 - val : val;
      if (nvalue < 0.0)
	nvalue = 0.0;
      
      nvalue = 0.5 * pow (2.0 * nvalue, getExponentContrast (contrast));
      val = (val > 0.5) ? 1.0 - nvalue : nvalue;
    }
    
    /* apply gamma */
    if (gamma != 1.0) {
      val = pow (val, getExponentGamma (gamma));
    }
    
    return val;
}

template<typename T>
struct brightness_contrast_gamma_template
{
  void operator() (Image& image, double brightness, double contrast, double gamma)
  {
    T it (image);
    typename T::accu a;
    typename T::accu::vtype _r, _g, _b;
    double r, g, b;
    
    for (int i = 0; i < image.h * image.w; ++i)
      {
	a = *it;
	
	a.getRGB (_r, _g, _b);
	r = _r, g = _g, b = _b;
	r /= T::accu::one().v[0];
	g /= T::accu::one().v[0];
	b /= T::accu::one().v[0];
	
	r = convert (r, brightness, contrast, gamma);
	g = convert (g, brightness, contrast, gamma);
	b = convert (b, brightness, contrast, gamma);
	
	_r = (typename T::accu::vtype)(r * T::accu::one().v[0]);
	_g = (typename T::accu::vtype)(g * T::accu::one().v[0]);
	_b = (typename T::accu::vtype)(b * T::accu::one().v[0]);
	a.setRGB (_r, _g, _b);
	it.set(a);
	++it;
      }
    image.setRawData();
  }
};

void brightness_contrast_gamma (Image& image, double brightness, double contrast, double gamma)
{
  codegen<brightness_contrast_gamma_template> (image, brightness, contrast, gamma);
}

template <typename T>
struct hue_saturation_lightness_template {
  void operator() (Image& image, double _hue, double saturation, double lightness)
  {
    T it (image);

    // optimized ONE2 in divisions imprecise shifts if not an FP type
    const typename T::accu::vtype ONE = T::accu::one().v[0],
                                  ONE2 = ONE > 1 ? ONE + 1 : ONE;

    // H in degree, S and L [-1, 1], latér scaled to the integer range
    _hue = fmod (_hue, 360);
    if (_hue < 0)
      _hue += 360;
    const typename T::accu::vtype
      hue = ONE * _hue / 360;

    for (int i = 0; i < image.h * image.w; ++i)
      {
	typename T::accu a = *it;	
	typename T::accu::vtype r, g, b;
	a.getRGB (r, g, b);

	// RGB to HSV
	typename T::accu::vtype h, s, v;
	{
	  const typename T::accu::vtype min = std::min (std::min (r, g), b);
	  const typename T::accu::vtype max = std::max (std::max (r, g), b);
	  const typename T::accu::vtype delta = max - min;
	  v = max;

	  if (delta == 0) {
	    h = 0;
	    s = 0;
	  }
	  else {
	    s = max == 0 ? 0 : ONE - (ONE * min / max);
	    
	    if (max == r) // yellow - magenta
	      h = (60*ONE/360) * (g - b) / delta + (g >= b ? 0 : ONE);
	    else if (max == g) // cyan - yellow
	      h = (60*ONE/360) * (b - r) / delta + 120*ONE/360;
	    else // magenta - cyan
	      h = (60*ONE/360) * (r - g) / delta + 240*ONE/360;
	  }
	}
	
	// hue should only be positive, se we only need to check one overflow
	h += hue;
	if (h >= ONE)
	  h -= ONE;
	
	// TODO: this might not be accurate, double check, ...
	s = s + s * saturation;
	s = std::max (std::min (s, ONE), (typename T::accu::vtype)0);
	
	v = v + v * lightness;
	v = std::max (std::min (v, ONE), (typename T::accu::vtype)0);
	
	// back from HSV to RGB
	{
	  const int i = 6 * h / ONE2;
	  
	  const typename T::accu::vtype f = 6 * h % ONE2;
	  // only compute the ones "on-demand" as needed
	  #define p (v * (ONE - s) / ONE2)
	  #define q (v * (ONE - f * s / ONE2) / ONE2)
	  #define t (v * (ONE - (ONE - f) * s / ONE2) / ONE2)

	  switch (i) {
	  case 0:
	    r = v;
	    g = t;
	    b = p;
	    break;
	  case 1:
	    r = q;
	    g = v;
	    b = p;
	    break;
	  case 2:
	    r = p;
	    g = v;
	    b = t;
	    break;
	  case 3:
	    r = p;
	    g = q;
	    b = v;
	    break;
	  case 4:
	    r = t;
	    g = p;
	    b = v;
	    break;
	  default: // case 5:
	    r = v;
	    g = p;
	    b = q;
	    break;
	  }
#undef p
#undef q
#undef t
	} // end

	a.setRGB (r, g, b);
	it.set(a);
	++it;
      }
    image.setRawData();
  }
};

void hue_saturation_lightness (Image& image, double hue, double saturation, double lightness)
{
  codegen<hue_saturation_lightness_template> (image, hue, saturation, lightness);
}

template <typename T>
struct invert_template
{
  void operator() (Image& image)
  {
    T it (image);
    
    for (int y = 0; y < image.h; ++y) {
      for (int x = 0; x < image.w; ++x) {
	
	typename T::accu a = *it;
	a = T::accu::one() -= a;
	it.set (a);
	++it;
      }
    }
    image.setRawData();
  }
};

void invert (Image& image)
{
  codegen<invert_template> (image);
}
