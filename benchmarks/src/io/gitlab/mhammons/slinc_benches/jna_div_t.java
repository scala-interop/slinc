package io.gitlab.mhammons.slinc_benches;

import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

@FieldOrder({"quot", "rem"})
public class jna_div_t extends Structure implements Structure.ByValue{
   public int quot;
   public int rem;
}
