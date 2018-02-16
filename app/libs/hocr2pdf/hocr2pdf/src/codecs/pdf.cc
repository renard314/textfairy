/*
 * Copyright (c) 2007 - 2009 Susanne Klaus <susanne@exactcode.de>
 * Copyright (c) 2008 - 2011 René Rebe <rene@exactcode.de>
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

#include "pdf.hh"

#ifndef WITHZLIB
#define WITHZLIB 1
#endif
#include "../utility/Encodings.hh"

#include "jpeg.hh"

#if WITHJASPER == 1
#include "jpeg2000.hh"
#endif

#include <string>
#include <sstream>

#include <vector>
#include <list>
#include <set>
#include <map>

/*
  Concept:
  
  * buffer drawing commands as they come
  * write out images, fonts, and other objects / streams immediately
  * write out bufferd page content at "endPage"
  * keep track of object id's and positions to write out at the end
  
  In theory we could use any resource name (some PDF writers use R*).
  However, for the prettification of it we use /F for Fonts and /I
  for images.
  
  We also format the stream quite readable, maybe we later (optionally)
  do not print most of the formating whitespace.
*/

/*
  Object types:
  
  * Catalog (Pages, Outlines, ...)
  * Outlines
  * Pages (Kids)
  * Page (Contents)
  * Font
  * XObject, Subtype Image
  
  * Number et al. (e.g. for stream length)
  
  * Resources
  * Xref
  * Trailer
*/

struct PDFObject; // fwd
struct PDFPage; // fwd
std::ostream& operator<< (std::ostream& s, PDFObject& obj); // fwd

// keeps track of objects, writes out xref obj table
struct PDFXref
{
  PDFXref()
    : imageCount(0), fontCount(0)
  {}
  
  void write(std::ostream& s);
  
  std::vector<PDFObject*> objects;
  uint64_t streamPos;
  
  // UUID of font and image references
  uint32_t imageCount, fontCount;
};

std::ostream& operator<< (std::ostream& s, PDFXref& obj)
{
  obj.write(s);
  return s;
}


// any PDF object, storing ID, generation and position in stream
struct PDFObject
{
  PDFObject(PDFXref& xref)
    : generation(0), streamPos(0)
  {
    xref.objects.push_back(this);
    id = xref.objects.size(); // after adding, 1-based
  }

  virtual ~PDFObject()
  {}

  void write(std::ostream& s)
  {
    // save position in stream for further reference
    s << "\n";
    streamPos = s.tellp();
    s << id << " " << generation << " obj\n";
    writeImpl(s);
    s << "endobj\n";
    
    while (!pendingObjWriteout.empty()) {
      PDFObject* obj = pendingObjWriteout.front();
      s << *obj;
      pendingObjWriteout.pop_front();
    }
  }
  
  virtual void writeImpl(std::ostream& s) = 0;
  
  // generate in-file indirect reference spec
  std::string indirectRef() const
  {
    std::stringstream s;
    s << id << " " << generation << " R";
    return s.str();
  }
  
  // due to C++ design bug, actually I would wante to have them pure virtual,
  // but then each derived class would need to define them anyway ... :-(
  virtual std::string resourceName() const {
    return "";
  }
  
  virtual std::string resourceType() const {
    return "";
  };
  
  uint64_t getStreamPos() const {
    return streamPos;
  }
  
  uint32_t id, generation;
  uint64_t streamPos;
  
  std::list<PDFObject*> pendingObjWriteout;
};

std::ostream& operator<< (std::ostream& s, PDFObject& obj)
{
  obj.write(s);
  return s;
}

struct PDFNumber : public PDFObject
{
  PDFNumber (PDFXref& xref)
    : PDFObject(xref)
  {}
  
  virtual void writeImpl(std::ostream& s) {
    s << value << "\n";
  }
  
  uint64_t value;
};

struct PDFStream : public PDFObject
{
  PDFStream (PDFXref& xref)
    : PDFObject(xref), number(xref)
  {}
  
  virtual void writeStreamTagsImpl(std::ostream& s) {
  }
  
  virtual void writeStreamImpl(std::ostream& s) {
  }
  
  virtual void writeImpl(std::ostream& s) {
    s << "<<\n";
    writeStreamTagsImpl(s);

    s << "/Length " << number.indirectRef() << "\n"
      ">>\n"
      "stream\n";
    
    uint64_t o1 = s.tellp(), o2;
    writeStreamImpl(s);
    s.flush();
    o2 = s.tellp();
    
    s << "\nendstream\n";
    
    number.value = o2 - o1;
    pendingObjWriteout.push_back (&number);
  }
  
  PDFNumber number;
};

// TODO: optional and obsolete, hardcode somewhere
struct PDFDocumentInfo : public PDFObject
{
  PDFDocumentInfo (PDFXref& xref)
    : PDFObject(xref)
  {}
  
  virtual void writeImpl(std::ostream& s) {
    
    /* TODO: Allow setting:
       /Title (...)
       /Author (...)
       ...
     */
    s << "<<\n"
      "/Creator (ExactImage)\n"
      "/Producer (ExactImage)\n"
      ">>\n";
  }
};

struct PDFPages : public PDFObject
{
  PDFPages (PDFXref& xref)
    : PDFObject(xref)
  {}
  
  virtual void writeImpl(std::ostream& s) 
  {
    // TODO: count needs to be determined by recursively scanning
    //       the whole, potentially nested, page hierarchy
    s << "<<\n"
      "/Type /Pages\n"
      "/Count " << pages.size() << "\n" // TODO
      "/Kids [";
    bool first = true;
    for (page_iterator it = pages.begin(); it != pages.end(); ++it) {
      s << (first ? "" : " ") << (*it)->indirectRef();
      first = false;
    }
    s << "]\n"
      ">>\n";
  }
  
  std::vector<PDFObject*> pages;
  typedef std::vector<PDFObject*>::iterator page_iterator;
};

struct PDFCatalog : public PDFObject
{
  PDFCatalog (PDFXref& xref, PDFPages& _pages)
    : PDFObject(xref), pages(_pages)
  {}
  
  virtual void writeImpl(std::ostream& s)
  {
    s << "<<\n"
      "/Type /Catalog\n"
      "/Pages " << pages.indirectRef() << "\n"
      ">>\n";
  }
  
  PDFPages& pages;
};

struct PDFFont : public PDFObject
{
  PDFFont(PDFXref& _xref, const std::string& _fontname = "Helvetica")
    : PDFObject(_xref), fontname(_fontname)
  {
    fontID = ++_xref.fontCount;
  }
  
  virtual std::string resourceName() const {
    std::stringstream s;
    s << "/F" << fontID;
    return s.str();
  }
  
  virtual std::string resourceType() const {
    return "/Font";
  }

  virtual void writeImpl(std::ostream& s)
  {
    // TODO: allow embedding and sub-setting, for now only built-in ones
    s << "<<\n"
      "/Type /Font\n"
      "/Subtype /Type1\n"
      "/BaseFont /" << fontname << "\n"
      // as FOP, for now, or: /PDFDocEncoding, /MacRomanEncoding etc.
      "/Encoding /WinAnsiEncoding\n"
      ">>\n";
  }
  
  std::string fontname;
  uint32_t fontID;
};

class Args
{
public:
  Args(const std::string& _args)
  {
    size_t it1 = 0;
    while (it1 < _args.size())
      {
	size_t it2 = _args.find_first_of(",;|:", it1);
	args.insert (_args.substr(it1, it2 - it1));
	
	if (it2 != std::string::npos)
	  it1 = it2 + 1;
	else
	  it1 = _args.size();
      }
  }
  
  bool contains(const std::string& arg)
  {
    if (args.find(arg) != args.end())
      return true;
    else
      return false;
  }
  
  void remove(const std::string& arg)
  {
    args.erase(arg);
  }
  
  bool containsAndRemove(const std::string& arg)
  {
    if (contains(arg)) {
      remove(arg);
      return true;
    }
    return false;
  }
  
  std::string str()
  {
    std::string ret;
    std::set<std::string>::iterator it = args.begin();
    if (it != args.end())
      ret = *it++;
    
    for (; it != args.end(); ++it) {
      ret += ",";
      ret += *it;
    }
    return ret;
  }
  
protected:
  std::set<std::string> args;
};

// so far always an image
struct PDFXObject : public PDFStream
{
  PDFXObject (PDFXref& _xref, Image& _image,
	      const std::string& _compress = "", int _quality = 80)
    : PDFStream(_xref), image(_image), compress(_compress), quality(_quality)
  {
    imageID = ++_xref.imageCount;
  }
  
  virtual std::string resourceName() const {
    std::stringstream s;
    s << "/I" << imageID;
    return s.str();
  }
  
  virtual std::string resourceType() const {
    return "/XObject";
  }
  
  virtual void writeStreamTagsImpl(std::ostream& s)
  {
    // default based on image type
    if (image.bps < 8) encoding = "/FlateDecode";
    else encoding = "/DCTDecode";
    
    // TODO: move transform to Args class
    std::string c(compress);
    std::transform(c.begin(), c.end(), c.begin(), tolower);
    Args args(c);
    
    if (args.containsAndRemove("ascii85"))
      encoding = "/ASCII85Decode";
    else if (args.containsAndRemove("hex"))
      encoding = "/ASCIIHexDecode";
#if WITHLIBJPEG == 1
    else if (args.containsAndRemove("jpeg"))
      encoding = "/DCTDecode";
#endif
#if WITHLIBJASPER == 1
    else if (args.containsAndRemove("jpeg2000"))
      encoding = "/JPXDecode";
#endif
    if (args.containsAndRemove("flate"))
      encoding = "/FlateDecode";
    compress = args.str();
    
    s << "/Type /XObject\n"
      "/Subtype /Image\n"
      "/Width " << image.w << " /Height " << image.h << "\n"
      "/ColorSpace " << (image.spp == 1 ? "/DeviceGray" : "/DeviceRGB") << "\n"
      "/BitsPerComponent " << image.bps << "\n"
      "/Filter " << encoding << "\n";
  }
  
  virtual void writeStreamImpl(std::ostream& s)
  {
    const int bytes = image.stride() * image.h;
    uint8_t* data = image.getRawData();
    
    if (encoding == "/FlateDecode")
      EncodeZlib(s, (const char*)data, bytes);
    if (encoding == "/ASCII85Decode")
      EncodeASCII85(s, data, bytes);
    else if (encoding == "/ASCIIHexDecode")
      EncodeHex(s, data, bytes);
    
#if WITHLIBJPEG == 1
    else if (encoding == "/DCTDecode") {
      ImageCodec::Write(&s, image, "jpeg", "jpg", quality, compress);
    }
#endif
#if WITHJASPER == 1
    else if (encoding == "/JPXDecode") {
      ImageCodec::Write(&s, image, "jp2", "jp2", quality, compress);
    }
#endif
    
    // TODO: let the codecs remove what they consumed
    Args args(compress);
    args.containsAndRemove("recompress");
    if (!args.str().empty())
      std::cerr << "PDFCodec: Unrecognized encoding option '"
		<< args.str() << "'" << std::endl;
  }
  
  uint32_t imageID;
  
  Image& image;
  std::string compress;
  std::string encoding;
  int quality;
};

struct PDFContentStream : public PDFStream
{
  PDFContentStream(PDFXref& _xref, PDFPage& _page)
    : PDFStream(_xref), parent(_page)
  {
    encoding = "/FlateDecode";
    c.setf(std::ios::fixed, std::ios::floatfield);
    c.setf(std::ios::showpoint);
    c.precision(8);
  }
  
  virtual void writeStreamTagsImpl(std::ostream& s)
  {
    if (!encoding.empty())
      s << "/Filter " << encoding << "\n";
  }
  
  virtual void writeStreamImpl(std::ostream& s)
  {
    if (!encoding.empty()) {
      EncodeZlib(s, (const char*)c.str().c_str(), c.str().size());
    }
    else {
      s << c.rdbuf();
    }
    
    c.str().clear(); // just release memory after writing
  }
  
  // for the beginning we translate the coordinates manually
  // to form a more commonly used up-down co-ordinate system
  void translateY(double& y);
  
  // drawing
  
  void moveTo(double x, double y)
  {
    translateY(y);
    last_x = x;
    last_y = y;
    c << x << " " << y << " m\n";
  }
  
  void addLineTo(double x, double y)
  {
    translateY(y);
    c << x << " " << y << " l\n";
  }

  void close()
  {
    c << "h\n";
  }
  
  void addCurveTo(double x1, double y1, double x2, double y2, double x3, double y3)
  {
    translateY(y1);
    translateY(y2);
    translateY(y3);
    
    c << x1 << " " << y1 << " "
      << x2 << " " << y2 << " "
      << x3 << " " << y3 << " c\n";
  }
  
  void setFillColor(double r, double g, double b)
  {
//    if ((r == g) && (g == b)) { // gray optimization for printers
//      c << r << " G\n"; // stroke
//      c << r << " g\n"; // fill
//    } else {
      c << r << " " << g << " " << b << " RG\n"; // stroke
      c << r << " " << g << " " << b << " rg\n"; // fill
//    }
  }
  
  void setLineWidth(double width)
  {
    c << width << " w\n";
  }
  
  void setLineDash(double offset, const std::vector<double>& dashes)
  {
    c << "[";
    for (unsigned i = 0; i < dashes.size(); ++i)
      c << " " << dashes[i];
    c << " ] " << offset << " d\n";
  }
  
  void setLineDash(double offset, const double* dashes, int n)
  {
    c << "[";
    for (; n; n--, dashes++)
      c << " " << *dashes;
    c << " ] " << offset << " d\n";
  }
  
  void showPath(PDFCodec::filling_rule_t fill = PDFCodec::fill_none)
  {
    switch (fill) {
    case PDFCodec::fill_non_zero:
      c << "f\n";
      break;
    case PDFCodec::fill_even_odd:
      c << "f*\n";
      break;
    default:
      c << "S\n"; // stroke
      break;
    }
  }
  
  void beginText();
  void endText();
  void textTo(double x, double y);
  void showText(const PDFFont& f, const std::string& text, double height);
  void showImage(const PDFXObject& i, double x, double y, double w, double h);
  
  PDFPage& parent;
  std::string encoding;
  std::stringstream c; // content / commands
  double last_x, last_y;
  double last_text_x, last_text_y, last_text_height;
  std::string last_text_res;
};

struct PDFPage : public PDFObject
{
  PDFPage(PDFXref& xref, PDFPages& _parent, double _w, double _h)
    : PDFObject(xref), parent(_parent), w(_w), h(_h), content(xref, *this)
  {
    parent.pages.push_back(this);
  }
  
  void addResource(const PDFObject* res)
  {
    if (res->resourceType() == "/Font")
      font_resources.insert(res);
    else
      image_resources.insert(res);
    // TODO: assert other types
  }
  
  virtual void writeImpl(std::ostream& s)
  {
    s << "<<\n"
      "/Type /Page\n"
      "/Parent " << parent.indirectRef() << "\n"
      "/MediaBox [0 0 " << w << " " << h << "]\n"
      "/Contents " << content.indirectRef() << "\n"
      "/Resources <<\n"
      // TODO: dnymically determin, though it's an obsolete info only tag
      "/ProcSet[/PDF /Text /ImageB /ImageC]\n";
      
    // for all utilzed indirect resources, /Font (F*) and /XObject (I*)
    if (!font_resources.empty())
      {
	s << (*font_resources.begin())->resourceType() << " <<";
	for (resource_iterator it = font_resources.begin();
	     it != font_resources.end(); ++it)
	  s << " " << (*it)->resourceName() << " " << (*it)->indirectRef();
	s << " >>\n";
      }
    if (!image_resources.empty())
      {
	s << (*image_resources.begin())->resourceType() << " <<";
	for (resource_iterator it = image_resources.begin();
	     it != image_resources.end(); ++it)
	  s << " " << (*it)->resourceName() << " " << (*it)->indirectRef();
	s << " >>\n";
      }
    
    s << ">>\n"
      ">>\n";
    pendingObjWriteout.push_back (&content);
    
    // just release memory after writing
    font_resources.clear();
    image_resources.clear();
  }
  
  PDFPages& parent;
  double w, h;
  PDFContentStream content;
  
  std::set<const PDFObject*> font_resources;
  std::set<const PDFObject*> image_resources;
  typedef std::set<const PDFObject*>::iterator resource_iterator;
};

// trailer with start of xref and EOF marker
struct PDFTrailer
{
  PDFTrailer(PDFXref& _xref, PDFObject& _root, PDFDocumentInfo* _info = 0)
    : xref(_xref), root(_root), info(_info)
  {}
  
  void write(std::ostream& s)
  {
    s << "\ntrailer\n"
      "<<\n"
      "/Size " << xref.objects.size() + 1 << "\n" // total number of entries
      "/Root " << root.indirectRef() << "\n";
    if (info)
      s << "/Info " << info->indirectRef() << "\n";
    
    s << ">>\n"
      "\nstartxref\n"
      << xref.streamPos << "\n"
      "%%EOF" << std::endl; // final flush, just in case
  }
  
  PDFXref& xref;
  PDFObject& root;
  PDFDocumentInfo* info;
};


void PDFContentStream::beginText()
{
  last_text_x = last_text_y = last_text_height = 0;
  last_text_res.clear();
  c << "BT\n";
}

// TODO: automatic tracking and assertion / auto-correction
void PDFContentStream::endText()
{
  c << "ET\n";
}

void PDFContentStream::textTo(double x, double y)
{
  translateY(y);
  c << x - last_text_x << " " << y - last_text_y << " Td\n";
  last_text_x = x;
  last_text_y = y;
}

void PDFContentStream::showText(const PDFFont& f, const std::string& text, double height)
{
  parent.addResource(&f);
  std::string fRes = f.resourceName();
  if (fRes != last_text_res || height != last_text_height) {
    c << f.resourceName() << " " << height << " Tf\n";
    last_text_height = height;
    last_text_res = fRes;
  }
  
  c <<  "(";
  
  // parse string and use proper escape
  // TODO: Unicode mappings
  bool first_newline = true;
  
  // decode utf8, locally
  std::vector<uint32_t> utf8 = DecodeUtf8(text.c_str(), text.size());
  for (std::vector<uint32_t>::const_iterator it = utf8.begin(); it != utf8.end(); ++it)
    {
      switch (*it)
	{
	  // escapes
	case '\\':
	case '(':
	case ')':
	  c << "\\" << (char)*it;
	  break;
	  
	  // newline
	case '\n':
	  c << ") Tj\n";
	  if (first_newline) {
	    first_newline = false;
	    c << height << " TL\n";
	  }
	  c << "T* (";
	  break;
	  
	  // just copy by default:
	default:
	  c << (char)*it;
	}
    }
  
  c << ") Tj\n";
}

void PDFContentStream::translateY(double& y)
{
  y = parent.h - y;
}

void PDFContentStream::showImage(const PDFXObject& i,
				 double x, double y, double w, double h)
{
  // add to resources
  parent.addResource(&i);
  c << "q\n"
    << "1 0 0 1 " << x << " " << y << " cm\n" // translate
    << w << " 0 0 " << h << " 0 0 cm\n" // scale
    << i.resourceName() << " Do\n"
    "Q\n";
}

std::ostream& operator<< (std::ostream& s, PDFTrailer& obj)
{
  obj.write(s);
  return s;
}


int PDFCodec::readImage (std::istream* stream, Image& image, const std::string& decompres)
{
    return false;
}

void PDFXref::write(std::ostream& s)
{
  s << "\n";
  
  // save position in stream for trailer
  streamPos = s.tellp();
  
  s << "xref\n"
    "0 " << objects.size() + 1 << "\n";
  
  for (unsigned int i = 0; i < objects.size() + 1; ++i)
    {
      uint32_t offset = 0;
      uint16_t generation = 0xFFFF;
      char state = 'f';
      if (i >0) {
	offset = objects[i-1]->getStreamPos();
	generation = 0;
	state = 'n';
      }
      s.fill('0');
      s.width(10);
      s << std::right << offset << " ";
      s.width(5);
      s << generation << " " << state << " \n"; // only double marker here
    }
}

struct PDFContext
{
  std::ostream* s;
  
  /* PDF stream construction */
  
  PDFXref xref;
  PDFDocumentInfo info;
  PDFPages pages;
  PDFCatalog catalog;
  PDFTrailer trailer;
  
  std::list<PDFPage*> pageList;
  PDFPage* currentPage;
  
  std::map<std::string, PDFFont*> fontMap;
  typedef std::map<std::string, PDFFont*>::iterator fontMapIterator;
  std::list<PDFXObject*> images;
  typedef std::list<PDFXObject*>::iterator imageIterator;
  
  PDFContext(std::ostream* _s)
    : s(_s), info(xref), pages(xref), catalog(xref, pages),
      trailer(xref, catalog, &info), currentPage(0)
  {
    // TODO: dynamic version depending on features used
    // TODO: place in some object?
    *s << "%PDF-1.4\n%\xc9\xcc\n"; // early 8-bit indicator cookie
    *s << info;
  }
  
  ~PDFContext()
  {
    // write out last page
    if (currentPage)
      *s << *currentPage;
    
    /* PDF stream finalizing */
    *s << pages;
    *s << catalog;
    *s << xref;
    *s << trailer;
    
    /* free dynamically allocated objects */
    while (!pageList.empty()) {
      PDFPage* p = pageList.front();
      delete (p);
      pageList.pop_front();
    }
    
    for (fontMapIterator it = fontMap.begin(); it != fontMap.end(); ++it)
      delete it->second;
    
    for (imageIterator it = images.begin(); it != images.end(); ++it)
      delete *it;
  }
  
  void beginPage(double w, double h)
  {
    // write out last page, also frees some memory
    if (currentPage)
      *s << *currentPage;
    
    currentPage = new PDFPage(xref, pages, w, h);
    pageList.push_back(currentPage);
  }
  
  PDFFont* getFont(const std::string& f)
  {
    PDFFont* font;
    fontMapIterator it = fontMap.find(f);
    if (it != fontMap.end())
      return it->second;
    
    font = new PDFFont(xref, f);
    *s << *font;
    return fontMap[f] = font;
  }
};

bool PDFCodec::writeImage (std::ostream* stream, Image& image, int quality,
			   const std::string& compress)
{
  PDFContext context(stream);
  
  PDFXObject* i = new PDFXObject(context.xref, image, compress, quality);
  *context.s << *i;
  context.images.push_back(i);
  
  const double iw = 72. * image.w / (image.resolutionX() ? image.resolutionX() : 72);
  const double ih = 72. * image.h / (image.resolutionY() ? image.resolutionX() : 72);

  context.beginPage(iw, ih);
  context.currentPage->content.showImage(*i, 0, 0, iw, ih);
  
  return true;
}

// external drawing API

// TODO: assert on each context ref

PDFCodec::PDFCodec(std::ostream* s)
{
  context = new PDFContext(s);
}

PDFCodec::~PDFCodec()
{
  if (context)
    delete context;
}

void PDFCodec::beginPage(double w, double h)
{
  context->beginPage(w, h);
}

void PDFCodec::moveTo(double x, double y)
{
  context->currentPage->content.moveTo(x, y);
}

void PDFCodec::addLineTo(double x, double y)
{
  context->currentPage->content.addLineTo(x, y);
}

void PDFCodec::closePath()
{
  context->currentPage->content.close();
}

void PDFCodec::addCurveTo(double x1, double y1, double x2, double y2,
			  double x3, double y3)
{
  context->currentPage->content.addCurveTo(x1, y1, x2, y2, x3, y3);
}

void PDFCodec::setFillColor(double r, double g, double b)
{
  context->currentPage->content.setFillColor(r, g, b);
}

void PDFCodec::setLineWidth(double width)
{
  context->currentPage->content.setLineWidth(width);
}

void PDFCodec::setLineDash(double offset, const std::vector<double>& dashes)
{
  context->currentPage->content.setLineDash(offset, dashes);
}

void PDFCodec::setLineDash(double offset, const double* dashes, int n)
{
  context->currentPage->content.setLineDash(offset, dashes, n);
}

void PDFCodec::showPath(filling_rule_t fill)
{
  context->currentPage->content.showPath(fill);
}

void PDFCodec::beginText()
{
  context->currentPage->content.beginText();
}

void PDFCodec::endText()
{
  context->currentPage->content.endText();
}

void PDFCodec::textTo(double x, double y)
{
  context->currentPage->content.textTo(x, y);
}

void PDFCodec::showText(const std::string& font, const std::string& text,
			double height)
{
  // hash and map font to PDFFont object
  PDFFont* f = context->getFont(font);
  context->currentPage->content.showText(*f, text, height);
}

void PDFCodec::showImage(Image& image, double x, double y,
			 double width, double height, int quality,
			 const std::string& compress)
{
  PDFXObject* i = new PDFXObject(context->xref, image, compress, quality);
  *context->s << *i;
  context->currentPage->content.showImage(*i, x, y, width, height);
  context->images.push_back(i);
}

PDFCodec pdf_loader;
