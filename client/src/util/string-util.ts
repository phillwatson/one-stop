/**
 * Generates a colour code based on the input string.
 * @param str the string to generate a colour for.
 * @returns the colour code.
 */
export function stringToColour(str: string) {
  let hash = 0;
  str.split('').forEach(char => { hash = char.charCodeAt(0) + ((hash << 5) - hash) })
  let colour = '#'
  for (let i = 0; i < 3; i++) {
    const value = (hash >> (i * 3)) & 0xff
    colour += value.toString(16).padStart(2, '0')
  }
  return colour
}
