package li.cil.oc.integration.appeng

import li.cil.oc.common.item.{Delegator, traits}

class ItemUpgradeAE(val parent: Delegator, val tier: Int) extends traits.Delegate with traits.ItemTier {}
