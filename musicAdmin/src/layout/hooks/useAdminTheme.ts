import { useDark, useToggle } from "@vueuse/core";
import { useMatrixTheme } from "./useMatrixTheme";

const DARK_THEME_INDEX = 0;
const LIGHT_THEME_INDEX = 1;

export function useAdminTheme() {
  const { setMatrixTheme, currentThemeIndex, initMatrixTheme } =
    useMatrixTheme();

  const isDark = useDark({
    selector: "html",
    attribute: "class",
    valueDark: "dark",
    valueLight: "",
    storageKey: "admin-color-scheme",
    initialValue: "dark"
  });
  const toggleDark = useToggle(isDark);

  function syncTheme() {
    const targetIndex = isDark.value ? DARK_THEME_INDEX : LIGHT_THEME_INDEX;
    if (currentThemeIndex.value !== targetIndex) {
      setMatrixTheme(targetIndex);
    } else {
      initMatrixTheme();
    }
  }

  function toggleTheme() {
    toggleDark();
    syncTheme();
  }

  return {
    isDark,
    toggleTheme,
    syncTheme
  };
}
