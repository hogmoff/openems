import { formatNumber } from '@angular/common';

export class Utils {

  constructor() { }

  /**
   * Returns true for last element of array
   * @param element
   * @param array
   */
  public static isLastElement(element, array: any[]) {
    return element == array[array.length - 1];
  }

  /**
   * Creates a deep copy of the object
   */
  public static deepCopy(obj: any, target?: any) {
    let copy: any;

    // Handle the 3 simple types, and null or undefined
    if (null == obj || "object" != typeof obj) return obj;

    // Handle Date
    if (obj instanceof Date) {
      if (target) {
        copy = target;
      } else {
        copy = new Date();
      }
      copy.setTime(obj.getTime());
      return copy;
    }

    // Handle Array
    if (obj instanceof Array) {
      if (target) {
        copy = target;
      } else {
        copy = [];
      }
      for (let i = 0, len = obj.length; i < len; i++) {
        copy[i] = this.deepCopy(obj[i]);
      }
      return copy;
    }

    // Handle Object
    if (obj instanceof Object) {
      if (target) {
        copy = target;
      } else {
        copy = {};
      }
      for (let attr in obj) {
        if (obj.hasOwnProperty(attr)) {
          copy[attr] = this.deepCopy(obj[attr], copy[attr]);
        }
      }
      return copy;
    }

    throw new Error("Unable to copy obj! Its type isn't supported.");
  }

  /**
   * Safely adds two - possibly 'null' - values: v1 + v2
   * 
   * @param v1 
   * @param v2 
   */
  public static addSafely(v1: number, v2: number): number {
    if (v1 == null) {
      return v2;
    } else if (v2 == null) {
      return v1;
    } else {
      return v1 + v2;
    }
  }

  /**
   * Safely subtracts two - possibly 'null' - values: v1 - v2
   * 
   * @param v1 
   * @param v2 
   */
  public static subtractSafely(v1: number, v2: number): number {
    if (v1 == null) {
      return v2;
    } else if (v2 == null) {
      return v1;
    } else {
      return v1 - v2;
    }
  }
  /**
   * Safely divides two - possibly 'null' - values: v1 / v2
   * 
   * @param v1 
   * @param v2 
   */
  public static divideSafely(v1: number, v2: number): number | null {
    if (v1 == null || v2 == null) {
      return null;
    } else if (v2 == 0) {
      return null; // divide by zero
    } else {
      return v1 / v2;
    }
  }

  /**
   * Safely multiplies two - possibly 'null' - values: v1 * v2
   * 
   * @param v1 
   * @param v2 
   */
  public static multiplySafely(v1: number, v2: number): number {
    if (v1 == null || v2 == null) {
      return null;
    } else {
      return v1 * v2;
    }
  }

  /**
   * Safely rounds a - possibly 'null' - value: Math.round(v)
   * 
   * @param v 
   */
  public static roundSafely(v: number): number {
    if (v == null) {
      return v;
    } else {
      return Math.round(v);
    }
  }

  /**
   * Gets the value; or if it is null, gets the 'orElse' value
   * 
   * @param v      the value or null
   * @param orElse the default value
   * @returns      the value or the default value
   */
  public static orElse(v: number, orElse: number): number {
    if (v == null) {
      return orElse;
    } else {
      return v;
    }
  }

  /**
   * Matches all filter-strings with all base-strings.
   * 
   * @param filters array of filter-strings
   * @param bases   array of base-strings
   * @returns       true if all filter strings exist in any base-strings
   */
  public static matchAll(filters: string[], bases: string[]): Boolean {
    for (let filter of filters) {
      let filterMatched = false;
      for (let base of bases) {
        if (base.includes(filter)) {
          filterMatched = true;
        }
      }
      if (!filterMatched) {
        return false;
      }
    }
    return true;
  }

  /**
   * Converts a value in Watt [W] to KiloWatt [kW].
   * 
   * @param value the value from passed value in html
   * @returns converted value
   */
  public static CONVERT_WATT_TO_KILOWATT = (value: any): string => {
    if (value == null) {
      return '-';
    }
    let thisValue: number = (value / 1000);

    if (thisValue >= 0) {
      return formatNumber(thisValue, 'de', '1.0-1') + ' kW';
    } else {
      return '0 kW';
    }
  }

  /**
   * Gets the image path for storage depending on State-of-Charge.
   * 
   * @param soc the state-of-charge
   * @returns the image path
   */
  public static getStorageSocImage(soc: number | null): string {
    if (!soc || soc < 10) {
      return 'storage_0.png';
    } else if (soc < 30) {
      return 'storage_20.png';
    } else if (soc < 50) {
      return 'storage_40.png';
    } else if (soc < 70) {
      return 'storage_60.png';
    } else if (soc < 90) {
      return 'storage_80.png';
    } else {
      return 'storage_100.png';
    }
  }

}