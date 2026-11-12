package com.botofan

import com.botofan.IOTProjLab.ProjLabMain

/**
 * Tries to return whether speciffic arguments were found or not
 * @param requiredIndex if you want that param to be found on a speciffic index, like 0
 * */
fun tryArg(
   args: Array<String>,
   vararg names: String,requiredIndex:Int = -1,
   onFound: (index: Int, name: String) -> Unit
): Boolean {
   for (name in names) {
      val i = args.indexOf(name)
      if (i != -1) {
         if(requiredIndex !=-1)
         {
            if(i == requiredIndex){
               onFound(i,name)
               return true
            }
            return false
         }else{
            onFound(i, name)
            return true
         }
      }
   }
   return false
}

fun main(args: Array<String>) {
   
   ProjLabMain.ProjectLabMain(args)
}





