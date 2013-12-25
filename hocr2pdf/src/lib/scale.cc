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

#include <string.h> // memset

#include <cmath>
#include <iostream>
#include <algorithm>

#include "Image.hh"
#include "ImageIterator2.hh"
#include "../codecs/Codecs.hh"

#include "Colorspace.hh"

#include "scale.hh"

void scale (Image& image, double scalex, double scaley)
{
  if (scalex == 1.0 && scaley == 1.0)
    return;
  
  // thru the codec?
  if (!image.isModified() && image.getCodec())
    if (image.getCodec()->scale(image, scalex, scaley))
      return;
  
  if (scalex <= 0.5)
    box_scale (image, scalex, scaley);
  else
    bilinear_scale (image, scalex, scaley);
}

template <typename T>
struct nearest_scale_template
{
  void operator() (Image& new_image, double scalex, double scaley)
  {
    Image image;
    image.copyTransferOwnership (new_image);
    
    new_image.resize ((int)(scalex * (double) image.w),
		      (int)(scaley * (double) image.h));
    new_image.setResolution (scalex * image.resolutionX(),
			     scaley * image.resolutionY());
    
    #pragma omp parallel for schedule (dynamic, 16)
    for (int y = 0; y < new_image.h; ++y)
    {
      T dst (new_image);
      dst.at(0, y);

      T src (image);
      for (int x = 0; x < new_image.w; ++x) {
	const int bx = (int) (((double) x) / scalex);
	const int by = (int) (((double) y) / scaley);
	
	typename T::accu a;
	a  = *src.at (bx, by);
	dst.set (a);
	++dst;
      }
    }
  }
};

void nearest_scale (Image& image, double scalex, double scaley)
{
  if (scalex == 1.0 && scaley == 1.0)
    return;
  codegen<nearest_scale_template> (image, scalex, scaley);
}

template <typename T>
struct bilinear_scale_template
{
  void operator() (Image& new_image, double scalex, double scaley)
  {
    Image image;
    image.copyTransferOwnership (new_image);

    new_image.resize ((int)(scalex * (double) image.w),
		      (int)(scaley * (double) image.h));
    new_image.setResolution (scalex * image.resolutionX(),
			     scaley * image.resolutionY());
    
    #pragma omp parallel for schedule (dynamic, 16)
    for (int y = 0; y < new_image.h; ++y)
    {
      T dst (new_image);
      dst.at(0, y);

      const double by = (-1.0+image.h) * y / new_image.h;
      const int sy = (int)floor(by);
      const int ydist = (int) ((by - sy) * 256);
      const int syy = sy+1;

      T src (image);
      for (int x = 0; x < new_image.w; ++x) {
	const double bx = (-1.0+image.w) * x / new_image.w;
	const int sx = (int)floor(bx);
	const int xdist = (int) ((bx - sx) * 256);
	const int sxx = sx+1;

	typename T::accu a;
	a  = (*src.at (sx,  sy )) * ((256-xdist) * (256-ydist));
	a += (*src.at (sxx, sy )) * (xdist       * (256-ydist));
	a += (*src.at (sx,  syy)) * ((256-xdist) * ydist);
	a += (*src.at (sxx, syy)) * (xdist       * ydist);
	a /= (256 * 256);
	dst.set (a);
	++dst;
      }
    }
  }
};

void bilinear_scale (Image& image, double scalex, double scaley)
{
  if (scalex == 1.0 && scaley == 1.0)
    return;
  codegen<bilinear_scale_template> (image, scalex, scaley);
}

template <typename T>
struct box_scale_template
{
  void operator() (Image& new_image, double scalex, double scaley)
  {
    Image image;
    image.copyTransferOwnership (new_image);
    
    new_image.resize ((int)(scalex * (double) image.w),
		      (int)(scaley * (double) image.h));
    new_image.setResolution (scalex * image.resolutionX(),
			     scaley * image.resolutionY());
    
    T src (image);
    T dst (new_image);
  
    // prepare boxes
    std::vector<typename T::accu> boxes(new_image.w);
    std::vector<int> count(new_image.w);
    std::vector<int> bindex(new_image.w);
    for (int sx = 0; sx < image.w; ++sx)
      bindex[sx] = std::min ((int)(scalex * sx), new_image.w - 1);
    
    int dy = 0;
    for (int sy = 0; dy < new_image.h && sy < image.h; ++dy)
      {
	// clear for accumulation
	for (int x = 0; x < new_image.w; ++x) {
	  boxes[x] = typename T::accu();
	  count[x] = 0;
	}
	
	for (; sy < image.h && scaley * sy < dy + 1; ++sy) {
	  //      std::cout << "sy: " << sy << " from " << image.h << std::endl;
	  for (int sx = 0; sx < image.w; ++sx) {
	    //	std::cout << "sx: " << sx << " -> " << dx << std::endl;
	    const int dx = bindex[sx];
	    boxes[dx] += *src; ++src;
	    ++count[dx];
	  }
	}
	
	// set box
	//    std::cout << "dy: " << dy << " from " << new_image.h << std::endl;
	for (int dx = 0; dx < new_image.w; ++dx) {
	  //      std::cout << "setting: dx: " << dx << ", from: " << new_image.w
	  //       		<< ", count: " << count[dx] << std::endl;      
	  boxes[dx] /= count[dx];
	  dst.set (boxes[dx]);
	  ++dst;
	}
      }
  }
};

void box_scale (Image& image, double scalex, double scaley)
{
  if (scalex == 1.0 && scaley == 1.0)
    return;
  codegen<box_scale_template> (image, scalex, scaley);
}

inline Image::iterator CubicConvolution (int distance,
					 const Image::iterator& f0,
					 const Image::iterator& f1,
					 const Image::iterator& f2,
					 const Image::iterator& f3) 
{
  Image::iterator it = f0;
  it = ( /*(    f1 + f3 - f0   - f2 ) * distance * distance * distance
	   + (f0*2 + f2 - f1*2 - f3 ) * distance * distance
	   +*/ (  f2 - f1             ) * distance ) / (256)
    + f1;
  return it;
}

/* 0 0 0 0
   0 4 0 0
   0 0 -13.5 6
   0 0 6.1 -2.45 */

void bicubic_scale (Image& new_image, double scalex, double scaley)
{
  if (scalex == 1.0 && scaley == 1.0)
    return;

  Image image;
  image.copyTransferOwnership (new_image);
  
  new_image.resize ((int)(scalex * (double) image.w),
		    (int)(scaley * (double) image.h));
  new_image.setResolution (scalex * image.resolutionX(),
			   scaley * image.resolutionY());
  
  Image::iterator dst = new_image.begin();
  Image::iterator src = image.begin();

  Image::iterator r0 = image.begin();
  Image::iterator r1 = image.begin();
  Image::iterator r2 = image.begin();
  Image::iterator r3 = image.begin();

  for (int y = 0; y < new_image.h; ++y) {
    double by = .5+y / scaley;
    const int sy = std::min((int)by, image.h-1);
    const int ydist = (int) ((by - sy) * 256);
    
    const int sy0 = std::max(sy-1, 0);
    const int sy2 = std::min(sy+1, image.h-1);
    const int sy3 = std::min(sy+2, image.h-1);
    
    for (int x = 0; x < new_image.w; ++x) {
      
      const double bx = .5+x / scalex;
      const int sx = std::min((int)bx, image.w - 1);
      const int xdist = (int) ((bx - sx) * 256);
      
      const int sx0 = std::max(sx-1, 0);
      const int sx2 = std::min(sx+1, image.w-1);
      const int sx3 = std::min(sx+2, image.w-1);
      
      //      xdist = ydist = 0;
      r0 = CubicConvolution (xdist,
			     *src.at(sx0,sy0), *src.at(sx,sy0),
			     *src.at(sx2,sy0), *src.at(sx3,sy0));
      r1 = CubicConvolution (xdist,
			     *src.at(sx0,sy),  *src.at(sx,sy),
			     *src.at(sx2,sy),  *src.at(sx3,sy));
      r2 = CubicConvolution (xdist,
			     *src.at(sx0,sy2), *src.at(sx,sy2),
			     *src.at(sx2,sy2), *src.at(sx3,sy2));
      r3 = CubicConvolution (xdist,
			     *src.at(sx0,sy3), *src.at(sx,sy3),
			     *src.at(sx2,sy3), *src.at(sx3,sy3));
      
      dst.set (CubicConvolution (ydist, r0, r1, r2, r3));
      ++dst;
    }
  }
}
#ifndef _MSC_VER

template <typename T>
struct ddt_scale_template
{
  void operator() (Image& new_image, double scalex, double scaley)
  {
    Image image;
    image.copyTransferOwnership (new_image);
    
    new_image.resize((int)(scalex * (double) image.w),
		     (int)(scaley * (double) image.h));
    new_image.setResolution (scalex * image.resolutionX(),
			     scaley * image.resolutionY());
    
    // first scan the source image and build a direction map
    char dir_map [image.h][image.w];
    
    T src_a(image), src_b(image), src_c(image), src_d(image);
    src_a.at(0, 1);
    src_b.at(1, 0);
    src_c.at(1, 1);
    
    for (int y = 0; y < image.h-1; ++y) {
      for (int x = 0; x < image.w-1; ++x) {
	typename T::accu::vtype a, b, c, d;
	(*src_a).getL(a); ++src_a;
	(*src_b).getL(b); ++src_b;
	(*src_c).getL(c); ++src_c;
	(*src_d).getL(d); ++src_d;
	
	//std::cout << "x: " << x << ", y: " << y << std::endl;
	//std::cout << "a: " << a << ", b: " << b
	//	  << ", c: " << c << ", d: " << d << std::endl;
	
	if (abs(a-c) < abs(b-d))
	  dir_map[y][x] = '\\';
	else
	  dir_map[y][x] = '/';
      }
      ++src_a; ++src_b; ++src_c; ++src_d;
    }
    
    if (false)
      {
	int n = 0;
	for (int y = 0; y < image.h-1; ++y) {
	  for (int x = 0; x < image.w-1; ++x) {
	    std::cout << (dir_map[y][x] == '/' ? ' ' : '\\');
	    if (dir_map[y][x] == '/') ++n;
	  }
	  std::cout << std::endl;
	}
	std::cout << "NW-SE: " << n << std::endl;
	
	std::cout << std::endl;
	
	n = 0;
	for (int y = 0; y < image.h-1; ++y) {
	  for (int x = 0; x < image.w-1; ++x) {
	    std::cout << (dir_map[y][x] == '/' ? '/' : ' ');
	    if (dir_map[y][x] != '/') ++n;
	  }
	  std::cout << std::endl;
	}
	std::cout << "NE-SW: " << n << std::endl;
      }
    
    T dst(new_image);
    T src(image);
    for (int y = 0; y < new_image.h; ++y) {
      const double by = (-1.0 + image.h) * y / new_image.h;
      const int sy = (int)floor(by);
      const int ydist = (int)((by - sy) * 256);
      
      for (int x = 0; x < new_image.w; ++x) {
	const double bx = (-1.0+image.w) * x / new_image.w;
	const int sx = (int)floor(bx);
	const int xdist = (int)((bx - sx) * 256);
	
	/*
	  std::cout << "bx: " << bx << ", by: " << by
	  << ", x: " << x << ", y: " << y
	  << ", sx: " << sx << ", sy: " << sy << std::endl;
	*/
	
	typename T::accu v;
	
	// which triangle does the point fall into?
	if (dir_map[sy][sx] == '/') {
	  const typename T::accu b = *src.at(sx, sy + 1);
	  const typename T::accu d = *src.at(sx + 1, sy);
	  
	  if (xdist <= 256 - ydist) // left side triangle
	    {
	      const typename T::accu a = *src.at(sx, sy);
	      // std::cout << "/-left" << std::endl;
	      v =  a       * (256 - xdist) * (256 - ydist);
	      v += b       * (256 - xdist) * ydist;
	      v += d       * xdist         * (256 - ydist);
	      v += (b+d)/2 * xdist         * ydist;
	    }
	  else // right side triangle
	    {
	      const typename T::accu c = *src.at(sx + 1, sy + 1);
	      //std::cout << "/-right" << std::endl;
	      v =  b       * (256 - xdist) * ydist;
	      v += c       * xdist         * ydist;
	      v += d       * xdist         * (256 - ydist);
	      v += (b+d)/2 * (256 - xdist) * (256 - ydist);
	    }
	}
	else {
	  const typename T::accu a = *src.at(sx, sy);
	  const typename T::accu c = *src.at(sx + 1, sy + 1);
	  if (xdist <= ydist) // left side triangle
	    {
	      const typename T::accu b = *src.at(sx, sy + 1);
	      //std::cout << "\\-left" << std::endl;
	      v =  a       * (256 - xdist) * (256 - ydist);
	      v += b       * (256 - xdist) * ydist;
	      v += c       * xdist         * ydist;
	      v += (a+c)/2 * xdist         * (256 - ydist);
	    }
	  else // right side triangle
	    {
	      const typename T::accu d = *src.at(sx + 1, sy);
	      //std::cout << "\\-right" << std::endl;
	      v =  a       * (256 - xdist) * (256 - ydist);
	      v += c       * xdist         * ydist;
	      v += d       * xdist         * (256 - ydist);
	      v += (a+c)/2 * (256 - xdist) * ydist;
	    }
	}
	v /= (256 * 256);
	dst.set(v);
	++dst;
      }
    }
  }
};
  
void ddt_scale (Image& image, double scalex, double scaley)
{
  if (scalex == 1.0 && scaley == 1.0)
    return;
  codegen<ddt_scale_template> (image, scalex, scaley);
}

#endif

void box_scale_grayX_to_gray8 (Image& new_image, double scalex, double scaley)
{
  if (scalex == 1.0 && scaley == 1.0)
    return;
  
  Image image;
  image.copyTransferOwnership (new_image);
  
  new_image.bps = 8; // always output 8bit gray
  new_image.resize((int)(scalex * (double) image.w),
		   (int)(scaley * (double) image.h));
  new_image.setResolution (scalex * image.resolutionX(),
			   scaley * image.resolutionY());
  uint8_t* src = image.getRawData();
  uint8_t* dst = new_image.getRawData();
  
#ifdef _MSC_VER
  std::vector<uint32_t> boxes(new_image.w);
  std::vector<uint32_t> count(new_image.w);
  std::vector<int> bindex(image.w);
#else
  uint32_t boxes[new_image.w];
  uint32_t count[new_image.w];
  // pre-compute box indexes
  int bindex [image.w];
#endif
  for (int sx = 0; sx < image.w; ++sx)
    bindex[sx] = std::min ((int)(scalex * sx), new_image.w - 1);
  
  const int bps = image.bps;
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
  
  int dy = 0;
  for (int sy = 0; dy < new_image.h && sy < image.h; ++dy)
    {
      // clear for accumulation
      memset (&boxes[0], 0, sizeof(boxes[0]) * new_image.w);
      memset (&count[0], 0, sizeof(count[0]) * new_image.w);
      
      for (; sy < image.h && sy * scaley < dy + 1; ++sy)
	{
	  uint8_t z = 0;
	  unsigned int bits = 0;
	  
	  for (int sx = 0; sx < image.w; ++sx)
	    {
	      if (bits == 0) {
		z = *src++;
		bits = 8;
	      }
	      
	      const int dx = bindex[sx];
	      boxes[dx] += gray_lookup[z >> bitshift];
	      ++count[dx];
	      
	      z <<= bps;
	      bits -= bps;
	    }
	}
      
      for (int dx = 0; dx < new_image.w; ++dx) {
	*dst = (boxes[dx] / count[dx]);
	++dst;
      }
    }
}

/*
void thumbnail_scale (Image& image, double scalex, double scaley)
{
  // only optimize the regular thumbnail down-scaling
  if (scalex > 1 || scaley > 1)
    return scale(image, scalex, scaley);
  
  // thru the codec?
  if (!image.isModified() && image.getCodec())
    if (image.getCodec()->scale(image, scalex, scaley))
      return;
  
  // quick sub byte scaling
  if (image.bps <= 8 && image.spp == 1) {
    box_scale_grayX_to_gray8(image, scalex, scaley);
  }
  else {
    if (image.spp == 1 && image.bps > 8)
      colorspace_by_name(image, "gray");
    else if (image.spp > 3 || image.bps > 8)
      colorspace_by_name(image, "rgb");
    
    box_scale(image, scalex, scaley);
  }

}
*/
