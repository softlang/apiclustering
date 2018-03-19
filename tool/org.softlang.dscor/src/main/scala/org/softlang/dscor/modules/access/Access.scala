package org.softlang.dscor.modules.access

import org.apache.spark.rdd.RDD
import org.softlang.dscor._
import org.softlang.dscor.utils.{InputStreamDelegate, Utils}

abstract class Access() extends RDDModule[(String, InputStreamDelegate)] {

}

