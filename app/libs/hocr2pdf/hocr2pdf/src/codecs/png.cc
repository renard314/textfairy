/*
 * Copyright (C) 2006 - 2009 René Rebe
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

#include <stdlib.h>
#include <png.h>

#include <iostream>

#include "png.hh"
#include "../utility/Endianess.hh"

#define png_infopp_NULL (png_infopp)NULL
#define int_p_NULL (int*)NULL
#define png_bytepp_NULL (png_bytepp)NULL
#define Z_BEST_COMPRESSION 100

void stdstream_read_data(png_structp png_ptr,
			 png_bytep data, png_size_t length)
{
  std::istream* stream = (std::istream*) png_get_io_ptr (png_ptr);
  stream->read ((char*)data, length);
}

void stdstream_write_data(png_structp png_ptr,
			  png_bytep data, png_size_t length)
{
  std::ostream* stream = (std::ostream*) png_get_io_ptr (png_ptr);
  stream->write ((char*)data, length);
}

void stdstream_flush_data(png_structp png_ptr)
{
  std::ostream* stream = (std::ostream*) png_get_io_ptr (png_ptr);
  stream = stream;
}


int PNGCodec::readImage (std::istream* stream, Image& image, const std::string& decompres)
{
  { // quick magic check
    char buf [4];
    stream->read (buf, sizeof (buf));
    int cmp = png_sig_cmp ((png_byte*)buf, (png_size_t)0, sizeof (buf));
    stream->seekg (0);
    if (cmp != 0)
      return false;
  }
  
  png_structp png_ptr;
  png_infop info_ptr;
  png_uint_32 width, height;
  int bit_depth, color_type, interlace_type;
  
  png_ptr = png_create_read_struct(PNG_LIBPNG_VER_STRING,
				   NULL /*user_error_ptr*/,
				   NULL /*user_error_fn*/,
				   NULL /*user_warning_fn*/);
  
  if (png_ptr == NULL)
    return 0;
  
  /* Allocate/initialize the memory for image information.  REQUIRED. */
  info_ptr = png_create_info_struct(png_ptr);
  if (info_ptr == NULL) {
    png_destroy_read_struct(&png_ptr, png_infopp_NULL, png_infopp_NULL);
    return 0;
  }
  
  /* Set error handling if you are using the setjmp/longjmp method (this is
   * the normal method of doing things with libpng).  REQUIRED unless you
   * set up your own error handlers in the png_create_read_struct() earlier.
   */
  
  if (setjmp(png_jmpbuf(png_ptr))) {
    /* Free all of the memory associated with the png_ptr and info_ptr */
    png_destroy_read_struct(&png_ptr, &info_ptr, png_infopp_NULL);
    /* If we get here, we had a problem reading the file */
    return 0;
  }
  
  /* Set up our STL stream input control */ 
  png_set_read_fn (png_ptr, stream, &stdstream_read_data);
  
  ///* If we have already read some of the signature */
  //png_set_sig_bytes(png_ptr, sig_read);
  
  /* The call to png_read_info() gives us all of the information from the
   * PNG file before the first IDAT (image data chunk).  REQUIRED
   */
  png_read_info (png_ptr, info_ptr);
  
  png_get_IHDR (png_ptr, info_ptr, &width, &height, &bit_depth, &color_type,
		&interlace_type, int_p_NULL, int_p_NULL);
  
  image.w = width;
  image.h = height;
  image.bps = bit_depth;
  image.spp = png_get_channels(png_ptr, info_ptr);

  png_uint_32 res_x, res_y;
  res_x = png_get_x_pixels_per_meter(png_ptr, info_ptr);
  res_y = png_get_y_pixels_per_meter(png_ptr, info_ptr);
  image.setResolution((2.54 * res_x + .5) / 100, (2.54 * res_y + .5) / 100);
  
  /* Extract multiple pixels with bit depths of 1, 2, and 4 from a single
   * byte into separate bytes (useful for paletted and grayscale images) */
  // png_set_packing(png_ptr);

  /* Change the order of packed pixels to least significant bit first
   * (not useful if you are using png_set_packing). */
  // png_set_packswap(png_ptr);

  /* Expand paletted colors into true RGB triplets */
  if (color_type == PNG_COLOR_TYPE_PALETTE) {
    png_set_palette_to_rgb(png_ptr);
    image.bps = 8;

	png_bytep trans_alpha;
	int num_trans;
	png_color_16p trans_color;
	png_get_tRNS(png_ptr, info_ptr, &trans_alpha, &num_trans, &trans_color);
	if (num_trans)
      image.spp = 4;
    else
      image.spp = 3;
  }
  
#if 0 // no longer needed
  /* Expand grayscale images to the full 8 bits from 2, or 4 bits/pixel */
  if (color_type == PNG_COLOR_TYPE_GRAY && bit_depth > 1 && bit_depth < 8) {
    png_set_gray_1_2_4_to_8(png_ptr);
    image.bps = 8;
  }
#endif  
  
  /* Expand paletted or RGB images with transparency to full alpha channels
   * so the data will be available as RGBA quartets.
   */
  if (png_get_valid(png_ptr, info_ptr, PNG_INFO_tRNS))
    png_set_tRNS_to_alpha(png_ptr);
  
  /* Set the background color to draw transparent and alpha images over.
   * It is possible to set the red, green, and blue components directly
   * for paletted images instead of supplying a palette index.  Note that
   * even if the PNG file supplies a background, you are not required to
   * use it - you should use the (solid) application background if it has one.
   */
#if 0
  png_color_16* image_background;
  if (png_get_bKGD(png_ptr, info_ptr, &image_background)) {
    png_set_background(png_ptr, image_background,
		       PNG_BACKGROUND_GAMMA_FILE, 1, 1.0);
    image.spp = 3;
  }
#endif
  
  /* If you want to shift the pixel values from the range [0,255] or
   * [0,65535] to the original [0,7] or [0,31], or whatever range the
   * colors were originally in:
   */
  if (png_get_valid(png_ptr, info_ptr, PNG_INFO_sBIT)) {
    png_color_8p sig_bit;
    
    png_get_sBIT(png_ptr, info_ptr, &sig_bit);
    png_set_shift(png_ptr, sig_bit);
  }

  /* swap bytes of 16 bit files to least significant byte first
     we store them in CPU byte order in memory */
  if (Exact::NativeEndianTraits::IsBigendian)
    png_set_swap(png_ptr);

  /* Turn on interlace handling.  REQURIED if you are not using
   * png_read_image().  To see how to handle interlacing passes,
   * see the png_read_row() method below:
   */
  int number_passes = png_set_interlace_handling (png_ptr);
  
  /* Optional call to gamma correct and add the background to the palette
   * and update info structure.  REQUIRED if you are expecting libpng to
   * update the palette for you (ie you selected such a transform above).
   */
  png_read_update_info(png_ptr, info_ptr);

  /* Allocate the memory to hold the image using the fields of info_ptr. */
  int stride = png_get_rowbytes (png_ptr, info_ptr);
  
  image.resize (image.w, image.h);
  png_bytep row_pointers[1];
  
  /* The other way to read images - deal with interlacing: */
  for (int pass = 0; pass < number_passes; ++pass)
    for (unsigned int y = 0; y < height; ++y) {
      row_pointers[0] = image.getRawData() + y * stride;
      png_read_rows(png_ptr, row_pointers, NULL, 1);
    }
  
  /* clean up after the read, and free any memory allocated - REQUIRED */
  png_destroy_read_struct(&png_ptr, &info_ptr, NULL);
  
  /* that's it */
  return true;
}

bool PNGCodec::writeImage (std::ostream* stream, Image& image, int quality,
			   const std::string& compress)
{
  png_structp png_ptr;
  png_infop info_ptr;
  
  png_ptr = png_create_write_struct(PNG_LIBPNG_VER_STRING,
				    NULL /*user_error_ptr*/,
				    NULL /*user_error_fn*/,
				    NULL /*user_warning_fn*/);
  
  if (png_ptr == NULL) {
    return false;
  }
  
  /* Allocate/initialize the memory for image information.  REQUIRED. */
  info_ptr = png_create_info_struct(png_ptr);
  if (info_ptr == NULL) {
    png_destroy_write_struct(&png_ptr, png_infopp_NULL);
    return false;
  }
  
  /* Set error handling if you are using the setjmp/longjmp method (this is
   * the normal method of doing things with libpng).  REQUIRED unless you
   * set up your own error handlers in the png_create_read_struct() earlier.
   */
  
  if (setjmp(png_jmpbuf(png_ptr))) {
    /* Free all of the memory associated with the png_ptr and info_ptr */
    png_destroy_write_struct(&png_ptr, &info_ptr);
    /* If we get here, we had a problem reading the file */
    return false;
  }
  quality = Z_BEST_COMPRESSION * (quality + Z_BEST_COMPRESSION) / 100;
  if (quality < 1) quality = 1;
  else if (quality > Z_BEST_COMPRESSION) quality = Z_BEST_COMPRESSION;
  png_set_compression_level(png_ptr, quality);

  //png_info_init (info_ptr);
  
  /* Set up our STL stream output control */ 
  png_set_write_fn (png_ptr, stream, &stdstream_write_data, &stdstream_flush_data);
  
  ///* If we have already read some of the signature */
  //png_set_sig_bytes(png_ptr, sig_read);
  
  int color_type;
  switch (image.spp) {
  case 1:
    color_type = PNG_COLOR_TYPE_GRAY;
    break;
  case 4:
    color_type = PNG_COLOR_TYPE_RGB_ALPHA;
    break;
  default:
    color_type = PNG_COLOR_TYPE_RGB;
  }
  
  png_set_IHDR (png_ptr, info_ptr, image.w, image.h, image.bps, color_type,
		PNG_INTERLACE_NONE, PNG_COMPRESSION_TYPE_DEFAULT,
		PNG_FILTER_TYPE_BASE);
  
  png_set_pHYs (png_ptr, info_ptr,
		(int)(image.resolutionX() * 100 / 2.54),
		(int)(image.resolutionY() * 100 / 2.54),
		PNG_RESOLUTION_METER);

  png_write_info (png_ptr, info_ptr);
  
  int stride = png_get_rowbytes (png_ptr, info_ptr);

  /* swap bytes of 16 bit data as PNG stores in network-byte-order */
  if (!Exact::NativeEndianTraits::IsBigendian)
    png_set_swap(png_ptr);
  
  png_bytep row_pointers[1]; 
  /* The other way to write images */
  int number_passes = 1;
  for (int pass = 0; pass < number_passes; ++pass)
    for (int y = 0; y < image.h; ++y) {
      row_pointers[0] = image.getRawData() + y * stride;
      png_write_rows(png_ptr, (png_byte**)&row_pointers, 1);
    }

  png_write_end(png_ptr, NULL);
  
  /* clean up after the read, and free any memory allocated - REQUIRED */
  png_destroy_write_struct(&png_ptr, &info_ptr);
  
  return true;
}

PNGCodec png_loader;
