package com.botofan

class InvalidPinException(pin:Int,additionalMessage:String? = null)
   : Exception(
   "Pin ${pin} is invalid!${
      additionalMessage?.let { " $additionalMessage" }
   }"
   )