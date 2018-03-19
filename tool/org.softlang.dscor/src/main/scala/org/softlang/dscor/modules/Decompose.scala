package org.softlang.dscor.modules

import org.softlang.dscor._
import org.softlang.dscor.modules.access.Access
import org.softlang.dscor.utils.InputStreamDelegate

abstract class Decompose(
    @Dependency var access: RDDModule[(String, InputStreamDelegate)]) extends RDDModule[(String, String)]  {
}