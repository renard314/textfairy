/*
 * Copyright (C) 2006 - 2010 René Rebe
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

#include "Codecs.hh"

#include <ctype.h> // tolower

#include <iostream>
#include <fstream>

std::vector<ImageCodec::loader_ref>* ImageCodec::loader = 0;

ImageCodec::ImageCodec ()
  : _image (0)
{
}

ImageCodec::ImageCodec (Image* __image)
  : _image (__image)
{
}

ImageCodec::~ImageCodec ()
{
  // if not freestanding
  if (!_image)
    unregisterCodec (this);
}

std::string ImageCodec::getExtension (const std::string& filename)
{
  // parse the filename extension
  std::string::size_type idx_ext = filename.rfind ('.');
  if (idx_ext && idx_ext != std::string::npos)
    return filename.substr (idx_ext + 1);
  else
    return "";
} 

std::string ImageCodec::getCodec (std::string& filename)
{
  // parse the codec spec, prefixed to the filename, e.g. jpg:, tif:, raw:, ...
  std::string::size_type idx_colon = filename.find (':');
  // unfortunately, Windows has this silly concept of drive "letters",
  // just limit the codec spec to anything longer than a single char
#ifdef _WIN32
  if (idx_colon && idx_colon < 2)
    idx_colon = filename.find (':', idx_colon + 1);
#endif
  if (idx_colon && idx_colon != std::string::npos) {
    std::string codec = filename.substr (0, idx_colon);
    filename.erase (0, idx_colon+1);
    return codec;
  }
  else
    return "";
} 

// NEW API

int ImageCodec::Read (std::istream* stream, Image& image,
		      std::string codec, const std::string& decompress,
		      int index)
{
  std::transform (codec.begin(), codec.end(), codec.begin(), tolower);
  
  std::vector<loader_ref>::iterator it;
  if (loader)
  for (it = loader->begin(); it != loader->end(); ++it)
    {
      if (codec.empty()) // try via magic
	{
	  // use primary entry to only try each codec once
	  if (it->primary_entry && !it->via_codec_only) {
	    int res = it->loader->readImage (stream, image, decompress, index);
            if (res > 0)
	    {
	      image.setDecoderID (it->loader->getID ());
	      return res;
	    }
	    // TODO: remove once the codecs are clean
	    stream->clear ();
	    stream->seekg (0);
	  }
	}
      else // manual codec spec
	{
	  if (it->primary_entry && it->ext == codec) {
	    return it->loader->readImage (stream, image, decompress, index);
	  }
	}
    }
  
  //std::cerr << "No matching codec found." << std::endl;
  return false;
}

bool ImageCodec::Write (std::ostream* stream, Image& image,
			std::string codec, std::string ext,
			int quality, const std::string& compress)
{
  std::transform (codec.begin(), codec.end(), codec.begin(), tolower);
  std::transform (ext.begin(), ext.end(), ext.begin(), tolower);
  
  std::vector<loader_ref>::iterator it;
  if (loader)
  for (it = loader->begin(); it != loader->end(); ++it)
    {
      if (codec.empty()) // match extension
	{
	  if (it->ext == ext)
	    goto do_write;
	}
      else // manual codec spec
	{
	  if (it->primary_entry && it->ext == codec) {
	    goto do_write;
	  }
	}
    }
  
  //std::cerr << "No matching codec found." << std::endl;
  return false;
  
 do_write:
  // reuse attached codec (if any and the image is unmodified)
  if (image.getCodec() && !image.isModified() && image.getCodec()->getID() == it->loader->getID())
    return (image.getCodec()->writeImage (stream, image, quality, compress));
  else
    return (it->loader->writeImage (stream, image, quality, compress));
}

ImageCodec* ImageCodec::MultiWrite (std::ostream* stream,
				    std::string codec, std::string ext)
{
  std::transform (codec.begin(), codec.end(), codec.begin(), tolower);
  std::transform (ext.begin(), ext.end(), ext.begin(), tolower);
  
  std::vector<loader_ref>::iterator it;
  if (loader)
  for (it = loader->begin(); it != loader->end(); ++it)
    {
      if (codec.empty()) // match extension
	{
	  if (it->ext == ext)
	    goto do_write;
	}
      else // manual codec spec
	{
	  if (it->primary_entry && it->ext == codec) {
	    goto do_write;
	  }
	}
    }
  
  //std::cerr << "No matching codec found." << std::endl;
  return 0;
  
 do_write:
  // TODO: reuse attached codec (if any and the image is unmodified)
  return it->loader->instanciateForWrite(stream);
}

// OLD API

int ImageCodec::Read (std::string file, Image& image, const std::string& decompress, int index)
{
  std::string codec = getCodec (file);
  
  std::istream* s;
  if (file != "-")
    s = new std::ifstream (file.c_str(), std::ios::in | std::ios::binary);
  else
    s = &std::cin;
  
  if (!*s) {
    //std::cerr << "Can not open file " << file.c_str() << std::endl;
    return false;
  }
  
  int res = Read (s, image, codec, decompress, index);
  if (s != &std::cin)
    delete s;
  return res;
}
  
bool ImageCodec::Write (std::string file, Image& image,
			int quality, const std::string& compress)
{
  std::string codec = getCodec (file);
  std::string ext = getExtension (file);
  
  std::ostream* s;
  if (file != "-")
    s = new std::ofstream (file.c_str(), std::ios::out | std::ios::binary);
  else
    s = &std::cout;
  
  if (!*s) {
    //std::cerr << "Can not write file " << file.c_str() << std::endl;
    return false;
  }
  
  bool res = Write (s, image, codec, ext, quality, compress);
  if (s != &std::cout)
    delete s;
  return res;
}

void ImageCodec::registerCodec (const char* _ext, ImageCodec* _loader,
				bool _via_codec_only)
{
  static ImageCodec* last_loader = 0;
  if (!loader)
    loader = new std::vector<loader_ref>;
  
  loader_ref ref = {_ext, _loader, _loader != last_loader, _via_codec_only};
  loader->push_back(ref);
  last_loader = _loader;
}

void ImageCodec::unregisterCodec (ImageCodec* _loader)
{
  // sanity check
  if (!loader) {
    std::cerr << "unregisterCodec: no codecs, unregister impossible!" << std::endl;
  }
  
  // remove from array
  std::vector<loader_ref>::iterator it;
  for (it = loader->begin(); it != loader->end();)
    if (it->loader == _loader)
      it = loader->erase (it);
    else
      ++it;
  
  if (loader->empty()) {
    delete loader;
    loader = 0;
  }
}

int ImageCodec::readImage (std::istream* stream, Image& image,
			   const std::string& decompress)
{
  return 0;
}

int ImageCodec::readImage (std::istream* stream, Image& image,
			   const std::string& decompress, int index)
{
  if (index == 0)
    return readImage(stream, image, decompress);
  else
    return 0;
}

ImageCodec* ImageCodec::instanciateForWrite (std::ostream* stream)
{
  return 0;
}

bool ImageCodec::Write (Image& image,
			int quality, const std::string& compress, int index)
{
  return false;
}

/*bool*/ void ImageCodec::decodeNow (Image* image)
{
  // intentionally left blank
}

// optional, return false (unsupported) by default
bool ImageCodec::flipX (Image& image)
{
  return false;
}

bool ImageCodec::flipY (Image& image)
{
  return false;
}

bool ImageCodec::rotate (Image& image, double ayngle)
{
  return false;
}

bool ImageCodec::crop (Image& image, unsigned int x, unsigned int y, unsigned int w, unsigned int h)
{
  return false;
}

bool ImageCodec::toGray (Image& image)
{
  return false;
}

bool ImageCodec::scale (Image& image, double xscale, double yscale)
{
  return false;
}
