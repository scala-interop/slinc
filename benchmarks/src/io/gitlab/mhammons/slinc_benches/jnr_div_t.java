package io.gitlab.mhammons.slinc_benches;

import jnr.ffi.Struct;
import jnr.ffi.Runtime;


public class jnr_div_t extends Struct{
   public Struct.Signed32 quot = new Struct.Signed32();
   public Struct.Signed32 rem = new Struct.Signed32();

   public jnr_div_t(Runtime runtime) {
      super(runtime);
   }
}