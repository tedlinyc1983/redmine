package ru.spb.itolia.redmine.ui;

import java.util.ArrayList;
import java.util.List;

import ru.spb.itolia.redmine.R;
import ru.spb.itolia.redmine.R.drawable;
import ru.spb.itolia.redmine.R.id;
import ru.spb.itolia.redmine.R.layout;
import ru.spb.itolia.redmine.R.string;
import ru.spb.itolia.redmine.R.style;
import ru.spb.itolia.redmine.api.RedmineApiManager;
import ru.spb.itolia.redmine.api.beans.Project;
import ru.spb.itolia.redmine.api.beans.RedmineHost;
import ru.spb.itolia.redmine.db.RedmineDBAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;


public class ProjectsActivity extends SherlockListActivity implements OnNavigationListener {
    //private SharedPreferences settings;
    //private final String PREFS_NAME = "prefs";
    MenuItem refresh;
	//private ProjectsAdapter mAdapter;
	//TODO: возможно, инициализировать новый объект RedmineDBAdapter тут, чтобы не создавать каждый раз.
    RedmineDBAdapter DBAdapter; // = new RedmineDBAdapter(ProjectsActivity.this.getApplicationContext())
    List<Object> spinner = new ArrayList<Object>();

	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	setTheme(R.style.Theme_Sherlock_Light_DarkActionBar);
    	super.onCreate(savedInstanceState);
    	DBAdapter = new RedmineDBAdapter(ProjectsActivity.this.getApplicationContext());
    	DBAdapter.open();
    	List<RedmineHost> hosts = DBAdapter.getHosts();
    	RedmineHost currentHost = DBAdapter.getHost(getIntent().getIntExtra("host_id", -1));
    	DBAdapter.close();
    	for(RedmineHost host: hosts) {
    		spinner.add(host);
    	}
    	spinner.add(getString(R.string.showHosts));
    	HostsAdapter adapter = new HostsAdapter(this, spinner);
        ActionBar actionBar = getSupportActionBar();
    	actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
    	actionBar.setListNavigationCallbacks(adapter, this);
    	ProjectsDBTask task = new ProjectsDBTask();
    	task.execute(getIntent().getIntExtra("host_id", -1));
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	//TODO: добавить выбор сервера (выпадающее меню с "учетными записями" серверов)
        menu.add(Menu.NONE, R.string.action_bar_new_project, Menu.NONE, getString(R.string.action_bar_new_project))
        .setIcon(R.drawable.new_project)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        
    	
    	refresh = menu.add(Menu.NONE, R.string.action_bar_refresh, Menu.NONE, getString(R.string.action_bar_refresh));
        refresh.setIcon(R.drawable.refresh);
        refresh.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    	

    	
      	return true;
    }
    
    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
    	if(spinner.get(itemPosition).getClass() == RedmineHost.class) {
    		RedmineHost p = (RedmineHost) spinner.get(itemPosition);
        	ProjectsDBTask task = new ProjectsDBTask();
        	task.execute(p.getId());
    		Toast toast = Toast.makeText(getApplicationContext(), "selected " + p.getLabel(), Toast.LENGTH_SHORT);
    		toast.show();
    	} else {
    		Intent intent = new Intent(this, RedmineHostsActivity.class);
    		intent.putExtra("host_id", getIntent().getIntExtra("host_id", -1));
    		startActivity(intent);
    	}
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
    	int selectedItemId = item.getItemId();
    	switch (selectedItemId) {
		case R.string.action_bar_new_project:
			Toast.makeText(this, "new project", Toast.LENGTH_SHORT).show();
			Intent intent = new Intent(this, NewProjectActivity.class);
			startActivity(intent);
			break;

		case R.string.action_bar_refresh:
			Toast.makeText(this, "Refreshing projects", Toast.LENGTH_SHORT).show();
			//TODO: Сделать нормальную анимацию
			refresh.setIcon(R.drawable.spinner);
	    	ProjectsTask task = new ProjectsTask();
	    	task.execute(getIntent().getIntExtra("host_id", -1));
			break;

		default:
			Toast.makeText(this, "nothing. " + item.getItemId(), Toast.LENGTH_SHORT).show();
			break;
		}
    	return false;
    }
 
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Project item = (Project) getListAdapter().getItem(position);
		String project_id = item.getIdentifier();
		Intent intent = new Intent(this, IssuesActivity.class);
		intent.putExtra("projectId", project_id);
		startActivity(intent);
	}
    
    private class ProjectsAdapter extends ArrayAdapter<Project> {
    	private Context context;
        private List<Project> projects;
        //private LayoutInflater mInflater;

 
		public ProjectsAdapter(Context context, List<Project> objects) {
			super(context, R.layout.project_row, objects);
			this.projects = objects;
			this.context = context;
		}

		class ViewHolder { // TODO fix static
			public TextView projectName;
			public TextView projectDesc;
			public TextView createdOn;
			public TextView updatedOn;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View rowView = convertView;
			if (rowView == null) {
				LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				rowView = inflater.inflate(R.layout.project_row, null);
				ViewHolder viewHolder = new ViewHolder();
				viewHolder.projectName = (TextView) rowView
						.findViewById(R.id.projectName);
				viewHolder.projectDesc = (TextView) rowView
						.findViewById(R.id.projectDesc);
				viewHolder.createdOn = (TextView) rowView
						.findViewById(R.id.projectCreatedOn);
				viewHolder.updatedOn = (TextView) rowView
						.findViewById(R.id.projectUpdateOn);
				rowView.setTag(viewHolder);
			}

			ViewHolder holder = (ViewHolder) rowView.getTag();
			Project p = projects.get(position);
			holder.projectName.setText(p.getName());
			holder.projectDesc.setText(p.getDescription());
			holder.createdOn.setText(p.getCreated_on());
			holder.updatedOn.setText(p.getUpdated_on());

			return rowView;
		}
	}

    private class HostsAdapter extends ArrayAdapter<Object> {
    	private Context context;
        private List<Object> spinner;
        //private LayoutInflater mInflater;

 
		public HostsAdapter(Context context, List<Object> spinner) {
			super(context, R.layout.hosts_row_spinner, R.id.host_label, spinner);
			this.spinner = spinner;
			this.context = context;
		}

		class ViewHolder { // TODO fix static
			public TextView label;
			//public TextView host;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View rowView = convertView;
			if(rowView == null) {
				LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				rowView = inflater.inflate(R.layout.hosts_row_spinner, null);
				ViewHolder viewHolder = new ViewHolder();
				rowView.setTag(viewHolder);
				
			}
			
			if (spinner.get(position).getClass() == RedmineHost.class) {
				ViewHolder holder = (ViewHolder) rowView.getTag();
				//viewHolder.host = (TextView) rowView.findViewById(R.id.host_address);
				holder.label = (TextView) rowView.findViewById(R.id.host_label);
				RedmineHost p =(RedmineHost) spinner.get(position);
				//holder.host.setText(p.getAddress());
				holder.label.setText(p.getLabel());
			} else {
				ViewHolder holder = (ViewHolder) rowView.getTag();
				//viewHolder.host = (TextView) rowView.findViewById(R.id.host_address);
				holder.label = (TextView) rowView.findViewById(R.id.host_label);
				String s = (String) spinner.get(position);
				holder.label.setText(s);
			}



			return rowView;
		}
	}
    
    //Task to get data from server
    private class ProjectsTask extends AsyncTask<Integer, Void, List<Project>> {

		@Override
		protected void onPreExecute() {
			//TODO: Анимация загрузки на кнопке
		}
		
		@Override
		protected List<Project> doInBackground(Integer... params) {
			Integer host_id = params[0];
			DBAdapter.open();
			String api_key = DBAdapter.getApi_key(host_id);
			String host_addr = DBAdapter.getHost(host_id).getAddress();
			DBAdapter.close();
			RedmineApiManager mgr = new RedmineApiManager(host_addr);
			
			List<Project> projects = null;
			try {
				projects = mgr.getProjects(api_key);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			DBAdapter.open();
			System.out.println("Saving projects to DB");
			for(Project project: projects) {
				DBAdapter.saveProject(project, host_id);
			}
			DBAdapter.close();
			
			return projects;
		}
		
		@Override
		protected void onPostExecute(List<Project> projects) {

			System.out.println("taking data from web");
			ProjectsAdapter mAdapter = new ProjectsAdapter(ProjectsActivity.this, projects);
	        setListAdapter(mAdapter);
	        refresh.setIcon(R.drawable.refresh);
		}
		
	}
    
    //Task to get projects list from DB
    private class ProjectsDBTask extends AsyncTask<Integer, Void, List<Project>> {

		@Override
		protected List<Project> doInBackground(Integer... params) {
			Integer host_id = params[0];
			//DBAdapter = new RedmineDBAdapter(ProjectsActivity.this.getApplicationContext());
			DBAdapter.open();
			List<Project> projects = DBAdapter.getProjects(host_id);
			DBAdapter.close();
			
			return projects;
		}
		
		@Override
		protected void onPostExecute(List<Project> projects) {
			if(projects.isEmpty()) {
				//TODO check if it is ok to run asynctask from asynctask
				ProjectsTask task = new ProjectsTask();
				task.execute(getIntent().getIntExtra("host_id", -1));
			} else {
				ProjectsAdapter mAdapter = new ProjectsAdapter(ProjectsActivity.this, projects);
				setListAdapter(mAdapter);
			}
		}
    }
}
