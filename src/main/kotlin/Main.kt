package com.botofan

import com.botofan.IOTProjExam.DHTReadingExample
import com.botofan.IOTProjExam.MagneticSensor
import com.botofan.IOTProjExam.ProjExamMain
import com.botofan.IOTProjLab.Injector
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

fun printGeneralHelp(){
   println("""
      -h : Help
      -1 : Lab project
      -2 : DHT reading test
      -3 : Magnetic test
      -4 : Exam Project
   """.trimIndent()
   )
}

fun main(args: Array<String>) {
   
   if (tryArg(args, "-h", requiredIndex = 0) { index, _ ->
         printGeneralHelp()
         
      }) {
      return
   }
   if (tryArg(args, "-1") { index, _ ->
         val args = args.toMutableList()
         args.removeAt(index)
         
         ProjLabMain.ProjectLabMain(args.toTypedArray())
      }) {
      return
   }
   if (tryArg(args, "-2") { index, _ ->
         val args = args.toMutableList()
         args.removeAt(index)
         
         DHTReadingExample.DHTReadingExample(args.toTypedArray())
      }) {
      return
   }
   if (tryArg(args, "-3") { index, _ ->
         val args = args.toMutableList()
         args.removeAt(index)
         val pi = Injector.getPi4Context()
         
         var bcm = 23
         
         if (args.isNotEmpty()) {
            tryArg(args.toTypedArray(), "-pin") { index, _ ->
               bcm = args[index + 1].toInt()
            }
         }
         
         
         println(
            """
            Using BCM ${bcm}. To change, rerun the program also adding "-pin x"
         """.trimIndent()
         )
         
         MagneticSensor.startReading(pi, bcm)
      }) {
      return
   }
   if (tryArg(args, "-4") { index, _ ->
         val args = args.toMutableList()
         args.removeAt(index)
         
         ProjExamMain.ProjectExamMain(args.toTypedArray())
      }){
      return
   }
   

}





