import { defineComponent, Fragment, type PropType } from "vue";
import { hasPerms } from "@/utils/auth";

export default defineComponent({
  name: "Perms",
  props: {
    value: {
      type: [String, Array] as PropType<string | string[]>,
      default: []
    }
  },
  setup(props, { slots }) {
    return () => {
      if (!slots) return null;
      return hasPerms(props.value) ? (
        <Fragment>{slots.default?.()}</Fragment>
      ) : null;
    };
  }
});
