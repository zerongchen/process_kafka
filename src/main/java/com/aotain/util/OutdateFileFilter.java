package com.aotain.util;

import java.io.File;
import java.io.FilenameFilter;

public class OutdateFileFilter implements FilenameFilter {

	 private String prefix = "";
	 
	  public OutdateFileFilter(String prefix){
		  
		  this.prefix = prefix.toLowerCase();
	  }
	  public boolean isOutdate(String file){
		  if(this.prefix.compareTo(file.toLowerCase().substring(0, this.prefix.length())) > 0){
			  return true;
		  }else {
			  return false;
		  }
	  }
	  
	  public boolean accept(File dir,String fname){   
	    return isOutdate(fname);   
	  
	  }   
}
