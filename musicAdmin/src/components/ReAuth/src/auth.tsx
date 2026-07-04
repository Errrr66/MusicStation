import { defineComponent, Fragment, type PropType } from "vue";
import { hasAuth } from "@/router/utils";

export default defineComponent({
  name: "Auth",
  props: {
    value: {
      type: [String, Array] as PropType<string | string[]>,
      default: []
    }
  },
  setup(props, { slots }) {
    return () => {
      if (!slots) return null;
      return hasAuth(props.value) ? (
        <Fragment>{slots.default?.()}</Fragment>
      ) : null;
    };
  }
});
