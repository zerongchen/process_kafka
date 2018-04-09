package com.aotain.util;

import java.io.File;
import java.io.FilenameFilter;

public class CompressFilter implements FilenameFilter {

	 private String _ext = "";
	 
	  public CompressFilter(String ext){
		  
		  _ext = ext.toLowerCase();
	  }
	  public boolean isTxt(String file){   
	    if (file.toLowerCase().endsWith(_ext)){   
	      return true;   
	    }else{   
	      return false;   
	    }   
	  }
	  
	  public boolean accept(File dir,String fname){   
	    return isTxt(fname);   
	  
	  }   
}