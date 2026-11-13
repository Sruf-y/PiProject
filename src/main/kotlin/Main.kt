package com.botofan

import com.botofan.IOTProjExam.MagneticSensor
import com.botofan.IOTProjExam.ProjExamMain
import com.botofan.IOTProjLab.Injector
import com.botofan.IOTProjLab.ProjLabMain
import com.botofan.IOTProjLab.ProjLabMain.pi

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

fun generalHelp(): String{
   return """
      -h : Help
      -1 : Lab project
      -2 : Exam project
      -3 : Magnetic test
   """.trimIndent()
}

fun main(args: Array<String>) {
   
   if(tryArg(args,"-h") { index,_->
      generalHelp()
      
   }){
      return
   }
   if(tryArg(args,"-1") { index,_->
      val args = args.drop(index).toTypedArray()
      ProjLabMain.ProjectLabMain(args)
   }){
      return
   }
   if(tryArg(args,"-2") { index,_->
      val args = args.drop(index).toTypedArray()
      ProjExamMain.ProjectExamMain(args)
   }){
      return
   }
   if(tryArg(args,"-3") { index,_->
         val args = args.drop(index).toTypedArray()
         val pi = Injector.getPi4Context()
         MagneticSensor.startReading(pi)
      }){
      return
   }
   
   
}





