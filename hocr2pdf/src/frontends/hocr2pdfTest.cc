/*
 * The ExactImage library's hOCR to PDF command line frontend
 * Copyright (C) 2008 - 2009 René Rebe, ExactCODE GmbH Germany
 * Copyright (C) 2008 Archivista
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

#include <string.h>

#include <iostream>
#include <fstream>
#include <iomanip>
#include <cmath>
#include <cctype>

#include <vector>

#include "../utility/ArgumentList.hh"

//#include "config.h"

#include "../codecs/Codecs.hh"
#include "../codecs/pdf.hh"
#include "../codecs/jpeg.hh"
//
#include "../lib/hocr.hh"

#define VERSION = "hocr4android"

using namespace Utility;

int main(int argc, char* argv[])
{
  //JPEGCodec jpeg_loader;
  ArgumentList arglist(false);

  // setup the argument list
  Argument<bool> arg_help("h", "help", "display this help text and exit");
  arglist.Add(&arg_help);

  Argument<std::string> arg_input("i", "input",  "input image filename", 0, 1, true, true);
  arglist.Add(&arg_input);

  Argument<std::string> arg_output("o", "output",   "output PDF filename", 1, 1, true, true);
  arglist.Add(&arg_output);

  Argument<int> arg_resolution("r", "resolution", "resolution overwrite", 0, 1, true, true);
  arglist.Add(&arg_resolution);

  Argument<bool> arg_no_image("n", "no-image","do not place the image over the text", 0, 0, true, true);
  arglist.Add(&arg_no_image);

  Argument<bool> arg_sloppy_text("s", "sloppy-text", "sloppily place text, group words, do not draw single glyphs",0, 0, true, true);
  arglist.Add(&arg_sloppy_text);

  Argument<std::string> arg_text("t", "text","extract text, including trying to remove hyphens",0, 1, true, true);
  arglist.Add(&arg_text);

  // parse the specified argument list - and maybe output the Usage
  if (!arglist.Read(argc, argv) || arg_help.Get() == true)
    {
      std::cerr << "hOCR to PDF converter, version " << std::endl
		<< "Copyright (C) 2008-2009 René Rebe, ExactCODE" << std::endl
		<< "Copyright (C) 2008 Archivista" << std::endl
		<< "Usage:" << std::endl;

      arglist.Usage(std::cerr);
      return 1;
    }
//
  // load the image, if specified and possible

  Image image; image.w = image.h = 0;
  if (arg_input.Size())
    {
	  std::cerr << arg_input.Get();
      if (!ImageCodec::Read(arg_input.Get(), image)) {
    	  std::cerr << "Error reading input file." << std::endl;
    	  return 1;
      }
    }

  if (arg_resolution.Size())
    image.setResolution(arg_resolution.Get(), arg_resolution.Get());
  if (image.resolutionX() <= 0 || image.resolutionY() <= 0) {
    std::cerr << "Warning: Image x/y resolution not set, defaulting to: "
	      << 300 << std::endl;
    image.setResolution(300, 300);
  }
  unsigned int res = image.resolutionX();
  bool sloppy = arg_sloppy_text.Get();

  std::ofstream* txtStream = 0;
  if (arg_text.Size()) {
    txtStream = new std::ofstream(arg_text.Get().c_str());
  }

  std::ostringstream txt;

  std::ifstream myfile ("recttest.html");
  std::ofstream s(arg_output.Get().c_str(), std::fstream::out | std::fstream::app);
  PDFCodec* pdfContext = new PDFCodec(&s);
  pdfContext->beginPage(72. * image.w / res, 72. * image.h / res);
  pdfContext->setFillColor(0, 0, 0);
  hocr2pdf(myfile, pdfContext, res, sloppy,arg_no_image.Get(), &txt);

  std::string str =  txt.str();
  const char* chr = str.c_str();
  std::cerr<<chr;

  if (!arg_no_image.Get()){
    pdfContext->showImage(image, 0, 0, 72. * image.w / res, 72. * image.h / res,5);
  }

  myfile.close();

  delete pdfContext;
  if (txtStream) {
    txtStream->close();
    delete txtStream;
  }
  return 0;
}
