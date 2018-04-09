namespace java vsfridge

struct Article {
  1: string name,
  2: i32 amount,
  3: i32 price
}

service Order {
  i32 calcPrice(1: Article article, 2: i32 amount),
  Article buyArticles(1: Article article, 2: i32 price)
}