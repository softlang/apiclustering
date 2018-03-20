package org.softlang.dscor

import org.softlang.dscor.utils.JUtils

/**
  * Created by Johannes on 19.03.2018.
  */
object Paths {
  def results = JUtils.configuration("dataset") + "/Results.csv"

  def poms = JUtils.configuration("dataset") + "/Poms.csv"

  def clients = JUtils.configuration("dataset") + "/Clients.csv"

  def versionDelta = JUtils.configuration("dataset") + "/VersionDelta.csv"

  def metadata = JUtils.configuration("dataset") + "/Metadata.csv"

  def counts = JUtils.configuration("dataset") + "/Counts.csv"

  def extendedCounts = JUtils.configuration("dataset") + "/ExtendedCounts.csv"

  def roverLP14 = JUtils.configuration("dataset") + "/RoverLP14.csv"

  def haertelAL18 = JUtils.configuration("dataset") + "/HaertelAL18.csv"

}
