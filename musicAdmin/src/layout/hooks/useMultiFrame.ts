const MAP = new Map();

export const useMultiFrame = () => {
  function setMap(path: any, Comp: any) {
    MAP.set(path, Comp);
  }

  function getMap(path?: any) {
    if (path) {
      return MAP.get(path);
    }
    return [...MAP.entries()];
  }

  function delMap(path: any) {
    MAP.delete(path);
  }

  return {
    setMap,
    getMap,
    delMap,
    MAP
  };
};
