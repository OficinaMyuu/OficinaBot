declare module "twemoji-parser" {
  export type EmojiEntity = {
    indices: [number, number]
    text: string
    url: string
    type: "emoji"
  }

  export function parse(value: string): EmojiEntity[]
}
