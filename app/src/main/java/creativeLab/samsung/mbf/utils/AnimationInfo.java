package creativeLab.samsung.mbf.utils;

public class AnimationInfo {
    private String category_id;
    private String selected_category_id;
    private String title;
    private String file_name;
    private int thumbnail;
    private String contentsAddress;
    private String subtitleAddress;
    private int episode_num = 0;

    public String getID() {
        return category_id;
    }
    public void setID(String category_id) {
        this.category_id = category_id;
    }

    public String getSelected_categoryID() {
        return selected_category_id;
    }
    public void setSelected_categoryID(String selected_category_id) {
        this.selected_category_id = selected_category_id;
    }

    public String getTitle() {
        return title ;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public int getThumbnail() {
        return thumbnail;
    }
    public void setThumbnail(int thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getFileName() {
        return file_name;
    }
    public void setFileName(String file_name) {
        this.file_name = file_name;
    }

    public int getEpisodeNum() {
        return episode_num;
    }
    public void setEpisodeNum(int episode_num) {
        this.episode_num = episode_num;
    }

    public String getContentsAddress() {
        return contentsAddress;
    }
    public void setContentsAddres(String contentsAddress) {
        this.contentsAddress = contentsAddress;
    }

    public String getSubtitleAddress() {
        return subtitleAddress;
    }
    public void setSubtitleAddress(String subtitleAddress) {
        this.subtitleAddress = subtitleAddress;
    }
}