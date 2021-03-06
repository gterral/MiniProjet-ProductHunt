package fr.ec.producthunt.data.database;

import android.database.Cursor;
import fr.ec.producthunt.data.model.Post;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Mohammed Boukadir  @:mohammed.boukadir@gmail.com
 */
public class PostDao {

  private final ProductHuntDbHelper productHuntDbHelper;

  public PostDao(ProductHuntDbHelper productHuntDbHelper) {
    this.productHuntDbHelper = productHuntDbHelper;
  }

  public long save(Post post) {
    return productHuntDbHelper.getWritableDatabase()
        .insert(DataBaseContract.PostTable.TABLE_NAME, null, post.toContentValues());
  }

  public long getLastPostId() {
    String query = "SELECT "+DataBaseContract.PostTable.ID_COLUMN+" from "+ DataBaseContract.PostTable.TABLE_NAME +" order by "+DataBaseContract.PostTable.ID_COLUMN+" DESC limit 1";
    Cursor c = productHuntDbHelper.getWritableDatabase().rawQuery(query, null);

    if (c != null && c.moveToFirst()) {
      return c.getLong(0); //The 0 is the column index, we only have 1 column, so the index is 0
    }

    return 0;
  }

  public List<Post> retrievePosts() {

    Cursor cursor = productHuntDbHelper.getReadableDatabase()
        .query(DataBaseContract.PostTable.TABLE_NAME,
            DataBaseContract.PostTable.PROJECTIONS,
            null, null, null, null, DataBaseContract.PostTable.ORDER_BY_DATE);

    List<Post> posts = new ArrayList<>(cursor.getCount());

    if (cursor.moveToFirst()) {
      do {

        Post post = new Post();

        post.setId(cursor.getInt(0));
        post.setTitle(cursor.getString(1));
        post.setSubTitle(cursor.getString(2));
        post.setCommentCount(cursor.getInt(3));
        post.setUrlImage(cursor.getString(4));
        post.setPostUrl(cursor.getString(5));
        posts.add(post);


      } while (cursor.moveToNext());
    }

    return posts;
  }
}
