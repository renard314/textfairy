/*
 * Copyright (C) 2006 - 2008 René Rebe
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
#define WITHLIBJPEG 1
extern "C" {
#include "jpeglib.h"
#include "jerror.h"
#include "transupp.h"
}

#include <sstream>

#include "Codecs.hh"

class JPEGCodec : public ImageCodec {
public:
  
  JPEGCodec () {
    registerCodec ("jpeg", this);
    registerCodec ("jpg", this);
  };
  
  // freestanding
  JPEGCodec (Image* _image);
  
  virtual std::string getID () { return "JPEG"; };
  
  virtual int readImage (std::istream* stream, Image& image, const std::string& decompres);
  virtual bool writeImage (std::ostream* stream, Image& image,
			   int quality, const std::string& compress);
  
  // on-demand decoding
  virtual /*bool*/ void decodeNow (Image* image);
  
  // optional optimizing and/or lossless implementations
  virtual bool flipX (Image& image);
  virtual bool flipY (Image& image);
  virtual bool rotate (Image& image, double angle);
  virtual bool crop (Image& image, unsigned int x, unsigned int y, unsigned int w, unsigned int h);
  virtual bool toGray (Image& image);
  virtual bool scale (Image& image, double xscale, double yscale);
  
private:

  void parseExif (Image& image);
  void decodeNow (Image* image, int factor);
  
  // internals and helper
  bool readMeta (std::istream* stream, Image& image);
  bool doTransform (JXFORM_CODE code, Image& image,
		    std::ostream* stream = 0, bool to_gray = false, bool crop = false,
		    unsigned int x = 0, unsigned int y = 0, unsigned int w = 0, unsigned int h = 0);
  
  std::stringstream private_copy;
};
