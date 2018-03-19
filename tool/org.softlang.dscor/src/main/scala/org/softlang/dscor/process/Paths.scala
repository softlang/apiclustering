package org.softlang.dscor.process

import org.softlang.dscor.utils.JUtils

/**
  * Created by Johannes on 19.03.2018.
  */
object Paths {
  def clients = JUtils.configuration("api_baseline") + "/Clients.csv"

  def apiDelta = JUtils.configuration("dataset") + "/ApiDelta.csv"

  def metadata = JUtils.configuration("dataset") + "/Metadata.csv"

  def counts = JUtils.configuration("dataset") + "/Counts.csv"

  def roverLP14 = JUtils.configuration("dataset") + "/RoverLP14.csv"

  def haertelAL18 = JUtils.configuration("dataset") + "/HaertelAL18.csv"
}
