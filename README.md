# DevUtils

このプラグインは `DevUtils` に依存する Spigot/Bukkit 向けの Minecraft プラグインです。
様々なエンティティの取得方法などをまとめたものです。
また図形の描画メソッドもある。

---

## ✅ 使用方法

### 🔧 開発環境でのセットアップ

1. プロジェクトディレクトリ内に `libs` フォルダを作成（存在しない場合）
2. `DevUtils.jar` を `libs/` フォルダ内に配置
3. `build.gradle` に以下を追加：

gradle
```gradle
dependencies {
    compileOnly files("libs/DevUtils.jar")
}
```
maven
```
mavenのやり方はわからないので各自で調べて
```

---

### 🚀 サーバーでの使用方法

1. `plugin.yml` に以下を追記：

```yaml
depend: [DevUtils]
```

2. サーバーの `plugins` フォルダに以下の2つのファイルを配置：
   - DevUtilsの機能を使用するプラグイン `.jar`
   - `DevUtils.jar`

---

## 📁 プロジェクト構成例

```
YourPlugin/
├── build.gradle
├── libs/
│   └── DevUtils.jar
├── src/
│   └── main/
│       └── java/
│           └── your/
│               └── package/
│                   └── Main.java
├── plugin.yml
└── README.md
```

---

## 実際の使用例
```java
List<LivinEntity> target = List<LivingEntity> target = EntityUtils.getNearestLivingEntities(
  location, // 中心
  radius, // 半径
  count, // 取得する数
  player, // 自信を除外 (player == 自身)
  Arrays.asList(Player.class, Villager.class) // ホワイトリスト (リスト形式)
);
```
```java
List<LivingEntity> target = EntityUtils.getEntitiesInSight(
  player, // 起点
  10.0d, // 半径
  30, // fov (弧度法)
  5, // カウント
  player, // 自身を除外 (player == 自身)
  Arrays.asList(Player.class, Villager.class) // ホワイトリスト (リスト形式)
  false // 障害物を考慮しないか
);
```
```java
Draw.sphere(
  center, // 中心
  radius, // 半径
  particle, // パーティクル
  density, // 密度
  countPerPoint // １点に描画するパーティクルの数
);
```




